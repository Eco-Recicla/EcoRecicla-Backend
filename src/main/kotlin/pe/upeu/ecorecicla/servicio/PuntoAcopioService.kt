package pe.upeu.ecorecicla.servicio

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.ecorecicla.dominio.PuntoAcopioEntity
import pe.upeu.ecorecicla.dominio.RecursoNoEncontradoException
import pe.upeu.ecorecicla.repositorio.PuntoAcopioRepository
import pe.upeu.ecorecicla.web.dto.CrearPuntoAcopioRequest
import pe.upeu.ecorecicla.web.dto.PaginaRespuesta
import pe.upeu.ecorecicla.web.dto.PuntoAcopioDto

@Service
@Transactional(readOnly = true)
class PuntoAcopioService(
    private val puntoAcopioRepository: PuntoAcopioRepository
) {

    fun listarPaginado(zona: String?, pagina: Int, tamanio: Int): PaginaRespuesta<PuntoAcopioDto> {
        val pageable = PageRequest.of(pagina, tamanio, Sort.by("razonSocial").ascending())
        val resultado = if (!zona.isNullOrBlank()) {
            puntoAcopioRepository.findByZonaIgnoreCaseAndActivoTrue(zona.trim(), pageable)
        } else {
            puntoAcopioRepository.findByActivoTrue(pageable)
        }

        val contenido = resultado.content.map { entity ->
            PuntoAcopioDto(
                id = entity.id ?: 0L,
                razonSocial = entity.razonSocial,
                direccion = entity.direccion,
                zona = entity.zona,
                materialHabitual = entity.materialHabitual,
                secuencia = 0,
                atendido = false
            )
        }

        return PaginaRespuesta(
            contenido = contenido,
            pagina = resultado.number,
            tamanio = resultado.size,
            totalElementos = resultado.totalElements,
            totalPaginas = resultado.totalPages,
            ultima = resultado.isLast
        )
    }

    fun obtenerPorId(id: Long): PuntoAcopioEntity {
        return puntoAcopioRepository.findById(id)
            .orElseThrow { RecursoNoEncontradoException("Punto de acopio no encontrado con ID: $id") }
    }

    @Transactional
    fun crearPunto(request: CrearPuntoAcopioRequest): PuntoAcopioDto {
        val entity = PuntoAcopioEntity(
            razonSocial = request.razonSocial.trim(),
            direccion = request.direccion.trim(),
            zona = request.zona.trim(),
            materialHabitual = request.materialHabitual.trim(),
            activo = true
        )
        val guardado = puntoAcopioRepository.save(entity)
        return PuntoAcopioDto(
            id = guardado.id ?: 0L,
            razonSocial = guardado.razonSocial,
            direccion = guardado.direccion,
            zona = guardado.zona,
            materialHabitual = guardado.materialHabitual,
            secuencia = 0,
            atendido = false
        )
    }
}
