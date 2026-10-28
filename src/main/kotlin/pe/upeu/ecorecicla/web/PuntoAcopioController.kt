package pe.upeu.ecorecicla.web

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import pe.upeu.ecorecicla.servicio.PesajeService
import pe.upeu.ecorecicla.servicio.PuntoAcopioService
import pe.upeu.ecorecicla.web.dto.CrearPuntoAcopioRequest
import pe.upeu.ecorecicla.web.dto.PaginaRespuesta
import pe.upeu.ecorecicla.web.dto.PesajeDto
import pe.upeu.ecorecicla.web.dto.PuntoAcopioDto

@RestController
@RequestMapping("/api/v1/puntos")
class PuntoAcopioController(
    private val puntoAcopioService: PuntoAcopioService,
    private val pesajeService: PesajeService
) {

    @GetMapping
    fun listar(
        @RequestParam(required = false) zona: String?,
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanio: Int
    ): ResponseEntity<PaginaRespuesta<PuntoAcopioDto>> {
        val resultado = puntoAcopioService.listarPaginado(zona, pagina, tamanio)
        return ResponseEntity.ok(resultado)
    }

    @GetMapping("/{id}/pesajes")
    fun listarPesajes(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") pagina: Int,
        @RequestParam(defaultValue = "20") tamanio: Int
    ): ResponseEntity<PaginaRespuesta<PesajeDto>> {
        val resultado = pesajeService.listarPorPunto(id, pagina, tamanio)
        return ResponseEntity.ok(resultado)
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PUNTO_GESTIONAR')")
    fun crearPunto(
        @Valid @RequestBody request: CrearPuntoAcopioRequest
    ): ResponseEntity<PuntoAcopioDto> {
        val creado = puntoAcopioService.crearPunto(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(creado)
    }
}
