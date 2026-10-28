package pe.upeu.ecorecicla.web

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import pe.upeu.ecorecicla.seguridad.Usuario
import pe.upeu.ecorecicla.servicio.PesajeService
import pe.upeu.ecorecicla.web.dto.PesajeDto
import pe.upeu.ecorecicla.web.dto.PesajeRequest
import pe.upeu.ecorecicla.web.dto.PesajeUpdateRequest

@RestController
@RequestMapping("/api/v1/pesajes")
class PesajeController(
    private val pesajeService: PesajeService
) {

    @GetMapping("/{id}")
    fun obtenerPorId(@PathVariable id: Long): ResponseEntity<PesajeDto> {
        val pesaje = pesajeService.obtenerPorId(id)
        return ResponseEntity.ok(pesaje)
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PESAJE_CREAR')")
    fun registrar(
        @Valid @RequestBody request: PesajeRequest,
        @AuthenticationPrincipal usuario: Usuario
    ): ResponseEntity<PesajeDto> {
        val creado = pesajeService.registrarPesaje(request, usuario)
        return ResponseEntity.status(HttpStatus.CREATED).body(creado)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PESAJE_EDITAR')")
    fun corregir(
        @PathVariable id: Long,
        @Valid @RequestBody request: PesajeUpdateRequest,
        @AuthenticationPrincipal usuario: Usuario
    ): ResponseEntity<PesajeDto> {
        val actualizado = pesajeService.corregirPesaje(id, request, usuario)
        return ResponseEntity.ok(actualizado)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PESAJE_ANULAR')")
    fun anular(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "Anulado por supervisor") motivo: String,
        @AuthenticationPrincipal usuario: Usuario
    ): ResponseEntity<Void> {
        pesajeService.anularPesaje(id, motivo, usuario)
        return ResponseEntity.noContent().build()
    }
}
