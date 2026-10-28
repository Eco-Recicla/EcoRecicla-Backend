package pe.upeu.ecorecicla.web

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import pe.upeu.ecorecicla.dominio.EstadoRuta
import pe.upeu.ecorecicla.dominio.RecursoNoEncontradoException
import pe.upeu.ecorecicla.dominio.RutaEntity
import pe.upeu.ecorecicla.dominio.RutaPuntoEntity
import pe.upeu.ecorecicla.repositorio.PuntoAcopioRepository
import pe.upeu.ecorecicla.repositorio.RutaPuntoRepository
import pe.upeu.ecorecicla.repositorio.RutaRepository
import pe.upeu.ecorecicla.seguridad.Rol
import pe.upeu.ecorecicla.seguridad.UsuarioRepository
import pe.upeu.ecorecicla.web.dto.AsignarRutaRequest
import pe.upeu.ecorecicla.web.dto.PuntoAcopioDto
import pe.upeu.ecorecicla.web.dto.RutaDto
import pe.upeu.ecorecicla.web.dto.UsuarioResponse
import java.time.Clock
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/admin")
class AdminController(
    private val usuarioRepository: UsuarioRepository,
    private val puntoAcopioRepository: PuntoAcopioRepository,
    private val rutaRepository: RutaRepository,
    private val rutaPuntoRepository: RutaPuntoRepository,
    private val clock: Clock
) {

    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('USUARIO_GESTIONAR')")
    fun listarUsuarios(): ResponseEntity<List<UsuarioResponse>> {
        val lista = usuarioRepository.findAll().map { u ->
            UsuarioResponse(
                id = u.id ?: 0L,
                usuario = u.username,
                nombreCompleto = u.nombreCompleto,
                rol = u.rol.name,
                privilegios = u.obtenerPrivilegios().map { it.name }.toSet()
            )
        }
        return ResponseEntity.ok(lista)
    }

    @PutMapping("/usuarios/{id}/rol")
    @PreAuthorize("hasAuthority('USUARIO_GESTIONAR')")
    fun actualizarRol(
        @PathVariable id: Long,
        @RequestParam nuevoRol: String
    ): ResponseEntity<UsuarioResponse> {
        val usuario = usuarioRepository.findById(id)
            .orElseThrow { RecursoNoEncontradoException("Usuario no encontrado con ID: $id") }

        val rolEnum = try {
            Rol.valueOf(nuevoRol.uppercase().trim())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Rol inválido: $nuevoRol. Roles válidos: ${Rol.values().joinToString()}")
        }

        usuario.rol = rolEnum
        val guardado = usuarioRepository.save(usuario)

        return ResponseEntity.ok(
            UsuarioResponse(
                id = guardado.id ?: 0L,
                usuario = guardado.username,
                nombreCompleto = guardado.nombreCompleto,
                rol = guardado.rol.name,
                privilegios = guardado.obtenerPrivilegios().map { it.name }.toSet()
            )
        )
    }

    @PostMapping("/rutas/asignar")
    @PreAuthorize("hasAuthority('PUNTO_GESTIONAR')")
    fun asignarRuta(
        @Valid @RequestBody request: AsignarRutaRequest
    ): ResponseEntity<RutaDto> {
        val operario = usuarioRepository.findByUsername(request.usuario)
            .orElseThrow { RecursoNoEncontradoException("Usuario no encontrado: ${request.usuario}") }

        val hoy = LocalDate.now(clock)
        val ruta = rutaRepository.save(
            RutaEntity(
                fecha = hoy,
                estado = EstadoRuta.ABIERTA,
                usuarioAsignado = operario
            )
        )

        val puntosDto = mutableListOf<PuntoAcopioDto>()
        request.puntosIds.forEachIndexed { index, puntoId ->
            val punto = puntoAcopioRepository.findById(puntoId)
                .orElseThrow { RecursoNoEncontradoException("Punto no encontrado con ID: $puntoId") }

            val rp = rutaPuntoRepository.save(
                RutaPuntoEntity(
                    ruta = ruta,
                    punto = punto,
                    secuencia = index + 1,
                    atendido = false
                )
            )

            puntosDto.add(
                PuntoAcopioDto(
                    id = punto.id ?: 0L,
                    razonSocial = punto.razonSocial,
                    direccion = punto.direccion,
                    zona = punto.zona,
                    materialHabitual = punto.materialHabitual,
                    secuencia = rp.secuencia,
                    atendido = rp.atendido
                )
            )
        }

        val respuesta = RutaDto(
            id = ruta.id ?: 0L,
            fecha = ruta.fecha.toString(),
            estado = ruta.estado.name,
            usuarioAsignado = operario.username,
            puntos = puntosDto
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta)
    }
}
