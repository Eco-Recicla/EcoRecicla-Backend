package pe.upeu.ecorecicla.servicio

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.ecorecicla.dominio.*
import pe.upeu.ecorecicla.repositorio.PesajeRepository
import pe.upeu.ecorecicla.repositorio.PuntoAcopioRepository
import pe.upeu.ecorecicla.repositorio.RutaPuntoRepository
import pe.upeu.ecorecicla.repositorio.RutaRepository
import pe.upeu.ecorecicla.seguridad.Privilegio
import pe.upeu.ecorecicla.seguridad.Rol
import pe.upeu.ecorecicla.seguridad.Usuario
import pe.upeu.ecorecicla.web.dto.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class PesajeService(
    private val pesajeRepository: PesajeRepository,
    private val puntoAcopioRepository: PuntoAcopioRepository,
    private val rutaRepository: RutaRepository,
    private val rutaPuntoRepository: RutaPuntoRepository,
    private val clock: Clock
) {

    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    @Transactional(readOnly = true)
    fun listarPorPunto(puntoId: Long, pagina: Int, tamanio: Int): PaginaRespuesta<PesajeDto> {
        val pageable = PageRequest.of(pagina, tamanio, Sort.by("fechaHora").descending())
        val resultado = pesajeRepository.findByPuntoIdOrderByFechaHoraDesc(puntoId, pageable)

        val contenido = resultado.content.map { mapearDto(it) }

        return PaginaRespuesta(
            contenido = contenido,
            pagina = resultado.number,
            tamanio = resultado.size,
            totalElementos = resultado.totalElements,
            totalPaginas = resultado.totalPages,
            ultima = resultado.isLast
        )
    }

    @Transactional(readOnly = true)
    fun obtenerPorId(id: Long): PesajeDto {
        val entity = pesajeRepository.findById(id)
            .orElseThrow { RecursoNoEncontradoException("Pesaje no encontrado con ID: $id") }
        return mapearDto(entity)
    }

    fun registrarPesaje(request: PesajeRequest, usuario: Usuario): PesajeDto {
        // Idempotencia: si ya existe una operación previa con la misma clave de cliente, retornar existente
        if (!request.clientOperationId.isNullOrBlank()) {
            val existente = pesajeRepository.findByClientOperationId(request.clientOperationId.trim())
            if (existente.isPresent) {
                return mapearDto(existente.get())
            }
        }

        val puntoId = request.puntoId ?: throw ValidacionNegocioException("puntoId", "El puntoId es obligatorio")
        val punto = puntoAcopioRepository.findById(puntoId)
            .orElseThrow { RecursoNoEncontradoException("Punto de acopio no encontrado con ID: $puntoId") }

        // Identificar la ruta
        val hoy = LocalDate.now(clock)
        val ruta = if (request.rutaId != null) {
            rutaRepository.findById(request.rutaId)
                .orElseThrow { RecursoNoEncontradoException("Ruta no encontrada con ID: ${request.rutaId}") }
        } else {
            rutaRepository.findByFechaAndUsuarioAsignadoUsername(hoy, usuario.username)
                .orElseGet {
                    rutaRepository.findByFecha(hoy).firstOrNull { it.estado == EstadoRuta.ABIERTA }
                        ?: throw ReglaNegocioException("No se encontró una ruta activa para registrar el pesaje")
                }
        }

        // RN-01: No registrar ni corregir pesajes en una ruta ya cerrada
        if (ruta.estado == EstadoRuta.CERRADA) {
            throw ReglaNegocioException("No se puede registrar ni corregir ningún pesaje en una ruta ya cerrada")
        }

        // RN-03: El servidor calcula pesoNeto = pesoBruto - tara. Debe ser mayor que cero.
        val pesoBruto = request.pesoBruto ?: throw ValidacionNegocioException("pesoBruto", "El peso bruto es obligatorio")
        val tara = request.tara ?: throw ValidacionNegocioException("tara", "La tara es obligatoria")

        val netoCalc = BigDecimal(pesoBruto.toString())
            .subtract(BigDecimal(tara.toString()))
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()

        if (netoCalc <= 0.0) {
            throw ValidacionNegocioException("tara", "La tara debe ser menor que el peso bruto para que el peso neto sea mayor a cero")
        }

        val pesaje = PesajeEntity(
            punto = punto,
            ruta = ruta,
            fechaHora = LocalDateTime.now(clock),
            material = request.material?.trim() ?: "Plástico",
            pesoBruto = pesoBruto,
            tara = tara,
            pesoNeto = netoCalc,
            operario = usuario.username,
            estado = EstadoPesajeTipo.REGISTRADO,
            clientOperationId = request.clientOperationId?.trim()
        )

        val guardado = pesajeRepository.save(pesaje)

        // Marcar el punto como atendido en la ruta
        val rutaPuntoOpt = rutaPuntoRepository.findByRutaIdAndPuntoId(ruta.id ?: 0L, punto.id ?: 0L)
        if (rutaPuntoOpt.isPresent) {
            val rp = rutaPuntoOpt.get()
            rp.atendido = true
            rp.fechaAtencion = LocalDateTime.now(clock)
            rutaPuntoRepository.save(rp)
        }

        return mapearDto(guardado)
    }

    fun corregirPesaje(id: Long, request: PesajeUpdateRequest, usuario: Usuario): PesajeDto {
        val pesaje = pesajeRepository.findById(id)
            .orElseThrow { RecursoNoEncontradoException("Pesaje no encontrado con ID: $id") }

        // RN-01: No corregir pesajes en ruta cerrada
        if (pesaje.ruta?.estado == EstadoRuta.CERRADA) {
            throw ReglaNegocioException("No se puede registrar ni corregir ningún pesaje en una ruta ya cerrada")
        }

        // RN-02: Un operario solo puede corregir los pesajes que él mismo registró y únicamente dentro del mismo día.
        // Si es de otro operario o fuera del plazo, el servicio responde 403.
        // El supervisor sí puede corregir otros pesajes.
        val esSupervisor = usuario.rol == Rol.SUPERVISOR
        if (!esSupervisor) {
            if (pesaje.operario != usuario.username) {
                throw AccessDeniedException("Un operario solo puede corregir los pesajes que él mismo registró")
            }

            val fechaPesaje = pesaje.fechaHora.toLocalDate()
            val hoy = LocalDate.now(clock)
            if (fechaPesaje != hoy) {
                throw AccessDeniedException("Un operario solo puede corregir pesajes registrados dentro del mismo día")
            }
        }

        // RN-03: Cálculo de peso neto > 0
        val pesoBruto = request.pesoBruto ?: throw ValidacionNegocioException("pesoBruto", "El peso bruto es obligatorio")
        val tara = request.tara ?: throw ValidacionNegocioException("tara", "La tara es obligatoria")

        val netoCalc = BigDecimal(pesoBruto.toString())
            .subtract(BigDecimal(tara.toString()))
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()

        if (netoCalc <= 0.0) {
            throw ValidacionNegocioException("tara", "La tara debe ser menor que el peso bruto para que el peso neto sea mayor a cero")
        }

        pesaje.material = request.material?.trim() ?: pesaje.material
        pesaje.pesoBruto = pesoBruto
        pesaje.tara = tara
        pesaje.pesoNeto = netoCalc

        val actualizado = pesajeRepository.save(pesaje)
        return mapearDto(actualizado)
    }

    fun anularPesaje(id: Long, motivo: String, usuario: Usuario) {
        val pesaje = pesajeRepository.findById(id)
            .orElseThrow { RecursoNoEncontradoException("Pesaje no encontrado con ID: $id") }

        if (pesaje.ruta?.estado == EstadoRuta.CERRADA) {
            throw ReglaNegocioException("No se puede anular un pesaje en una ruta ya cerrada")
        }

        pesaje.estado = EstadoPesajeTipo.ANULADO
        pesaje.motivoAnulacion = motivo.trim()
        pesaje.anuladoPor = usuario.username
        pesaje.fechaAnulacion = LocalDateTime.now(clock)
        pesajeRepository.save(pesaje)

        // Verificar si quedan otros pesajes válidos para este punto en la ruta
        val rutaId = pesaje.ruta?.id ?: 0L
        val puntoId = pesaje.punto?.id ?: 0L
        val rutaPuntoOpt = rutaPuntoRepository.findByRutaIdAndPuntoId(rutaId, puntoId)
        if (rutaPuntoOpt.isPresent) {
            val rp = rutaPuntoOpt.get()
            val otrosValidos = pesajeRepository.findByRutaIdAndPuntoIdAndEstado(
                rutaId,
                puntoId,
                EstadoPesajeTipo.REGISTRADO
            )
            if (otrosValidos.isEmpty() && rp.motivoOmision.isNullOrBlank()) {
                rp.atendido = false
                rutaPuntoRepository.save(rp)
            }
        }
    }

    private fun mapearDto(pesaje: PesajeEntity): PesajeDto {
        val estadoDto = when (pesaje.estado) {
            EstadoPesajeTipo.REGISTRADO -> EstadoPesajeDto(tipo = "REGISTRADO")
            EstadoPesajeTipo.ANULADO -> EstadoPesajeDto(tipo = "ANULADO", motivo = pesaje.motivoAnulacion)
        }

        return PesajeDto(
            id = pesaje.id ?: 0L,
            puntoId = pesaje.punto?.id ?: 0L,
            fechaHora = pesaje.fechaHora.format(isoFormatter),
            material = pesaje.material,
            pesoBruto = pesaje.pesoBruto,
            tara = pesaje.tara,
            pesoNeto = pesaje.pesoNeto,
            operario = pesaje.operario,
            estado = estadoDto,
            pendiente = false
        )
    }
}
