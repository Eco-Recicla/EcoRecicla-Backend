package pe.upeu.ecorecicla.servicio

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.ecorecicla.dominio.*
import pe.upeu.ecorecicla.repositorio.PesajeRepository
import pe.upeu.ecorecicla.repositorio.RutaPuntoRepository
import pe.upeu.ecorecicla.repositorio.RutaRepository
import pe.upeu.ecorecicla.seguridad.Rol
import pe.upeu.ecorecicla.seguridad.Usuario
import pe.upeu.ecorecicla.web.dto.PuntoAcopioDto
import pe.upeu.ecorecicla.web.dto.RutaDto
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class RutaService(
    private val rutaRepository: RutaRepository,
    private val rutaPuntoRepository: RutaPuntoRepository,
    private val pesajeRepository: PesajeRepository,
    private val clock: Clock
) {

    fun obtenerRutaDelDia(usuario: Usuario, fechaConsulta: LocalDate? = null): RutaDto {
        val fecha = fechaConsulta ?: LocalDate.now(clock)

        val ruta = if (usuario.rol == Rol.OPERARIO) {
            rutaRepository.findByFechaAndUsuarioAsignadoUsername(fecha, usuario.username)
                .orElseGet {
                    // Si no tiene asignación directa para hoy, buscar ruta abierta de hoy
                    rutaRepository.findByFecha(fecha).firstOrNull { it.estado == EstadoRuta.ABIERTA }
                        ?: throw RecursoNoEncontradoException("No se encontró una ruta asignada para el operario ${usuario.username} en la fecha $fecha")
                }
        } else {
            // Supervisor o Administrador: consultar ruta de la fecha indicada
            val rutas = rutaRepository.findByFecha(fecha)
            rutas.firstOrNull { it.estado == EstadoRuta.ABIERTA }
                ?: rutas.firstOrNull()
                ?: throw RecursoNoEncontradoException("No se encontraron rutas registradas para la fecha $fecha")
        }

        return mapearRutaDto(ruta)
    }

    fun obtenerPorId(id: Long): RutaDto {
        val ruta = rutaRepository.findById(id)
            .orElseThrow { RecursoNoEncontradoException("Ruta no encontrada con ID: $id") }
        return mapearRutaDto(ruta)
    }

    @Transactional
    fun registrarOmision(rutaId: Long, puntoId: Long, motivo: String, usuario: Usuario): RutaDto {
        val ruta = rutaRepository.findById(rutaId)
            .orElseThrow { RecursoNoEncontradoException("Ruta no encontrada con ID: $rutaId") }

        // RN-01: no se puede operar en ruta cerrada
        if (ruta.estado == EstadoRuta.CERRADA) {
            throw ReglaNegocioException("No se puede registrar una omisión en una ruta ya cerrada")
        }

        val rutaPunto = rutaPuntoRepository.findByRutaIdAndPuntoId(rutaId, puntoId)
            .orElseThrow { RecursoNoEncontradoException("El punto con ID $puntoId no pertenece a la ruta $rutaId") }

        rutaPunto.motivoOmision = motivo.trim()
        rutaPunto.omitidoPor = usuario.username
        rutaPunto.atendido = true
        rutaPunto.fechaAtencion = LocalDateTime.now(clock)
        rutaPuntoRepository.save(rutaPunto)

        return mapearRutaDto(ruta)
    }

    @Transactional
    fun cerrarRuta(rutaId: Long, usuario: Usuario): RutaDto {
        val ruta = rutaRepository.findById(rutaId)
            .orElseThrow { RecursoNoEncontradoException("Ruta no encontrada con ID: $rutaId") }

        if (ruta.estado == EstadoRuta.CERRADA) {
            throw ReglaNegocioException("La ruta ya se encuentra cerrada")
        }

        // RN-04: La ruta no puede cerrarse mientras algún punto planificado no tenga un pesaje registrado o un motivo de omisión
        val puntos = rutaPuntoRepository.findByRutaIdOrderBySecuenciaAsc(rutaId)
        val faltantes = mutableListOf<Long>()

        for (rp in puntos) {
            val tieneOmision = !rp.motivoOmision.isNullOrBlank()
            if (!tieneOmision) {
                val pesajesValidos = pesajeRepository.findByRutaIdAndPuntoIdAndEstado(
                    rutaId,
                    rp.punto?.id ?: 0L,
                    EstadoPesajeTipo.REGISTRADO
                )
                if (pesajesValidos.isEmpty()) {
                    faltantes.add(rp.punto?.id ?: 0L)
                }
            }
        }

        if (faltantes.isNotEmpty()) {
            throw ReglaNegocioException("La ruta no puede cerrarse mientras falten ${faltantes.size} punto(s) planificado(s) por registrar pesaje o motivo de omisión")
        }

        ruta.estado = EstadoRuta.CERRADA
        ruta.fechaCierre = LocalDateTime.now(clock)
        ruta.cerradaPor = usuario.username
        val cerrada = rutaRepository.save(ruta)

        return mapearRutaDto(cerrada)
    }

    private fun mapearRutaDto(ruta: RutaEntity): RutaDto {
        val rutaPuntos = rutaPuntoRepository.findByRutaIdOrderBySecuenciaAsc(ruta.id ?: 0L)
        val puntosDto = rutaPuntos.map { rp ->
            val punto = rp.punto!!
            PuntoAcopioDto(
                id = punto.id ?: 0L,
                razonSocial = punto.razonSocial,
                direccion = punto.direccion,
                zona = punto.zona,
                materialHabitual = punto.materialHabitual,
                secuencia = rp.secuencia,
                atendido = rp.atendido
            )
        }

        return RutaDto(
            id = ruta.id ?: 0L,
            fecha = ruta.fecha.toString(),
            estado = ruta.estado.name,
            usuarioAsignado = ruta.usuarioAsignado?.username ?: "",
            puntos = puntosDto
        )
    }
}
