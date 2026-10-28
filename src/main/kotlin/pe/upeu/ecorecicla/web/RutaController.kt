package pe.upeu.ecorecicla.web

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import pe.upeu.ecorecicla.seguridad.Usuario
import pe.upeu.ecorecicla.servicio.RutaService
import pe.upeu.ecorecicla.web.dto.RegistrarOmisionRequest
import pe.upeu.ecorecicla.web.dto.RutaDto
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/rutas")
class RutaController(
    private val rutaService: RutaService
) {

    @GetMapping("/hoy")
    fun rutaDelDia(
        @AuthenticationPrincipal usuario: Usuario,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fecha: LocalDate?
    ): ResponseEntity<RutaDto> {
        val ruta = rutaService.obtenerRutaDelDia(usuario, fecha)
        return ResponseEntity.ok(ruta)
    }

    @GetMapping("/{id}")
    fun obtenerPorId(@PathVariable id: Long): ResponseEntity<RutaDto> {
        val ruta = rutaService.obtenerPorId(id)
        return ResponseEntity.ok(ruta)
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize("hasAuthority('RUTA_CERRAR')")
    fun cerrarRuta(
        @PathVariable id: Long,
        @AuthenticationPrincipal usuario: Usuario
    ): ResponseEntity<RutaDto> {
        val cerrada = rutaService.cerrarRuta(id, usuario)
        return ResponseEntity.ok(cerrada)
    }

    @PostMapping("/{id}/puntos/{puntoId}/omision")
    fun registrarOmision(
        @PathVariable id: Long,
        @PathVariable puntoId: Long,
        @Valid @RequestBody request: RegistrarOmisionRequest,
        @AuthenticationPrincipal usuario: Usuario
    ): ResponseEntity<RutaDto> {
        val actualizada = rutaService.registrarOmision(id, puntoId, request.motivo, usuario)
        return ResponseEntity.ok(actualizada)
    }
}
