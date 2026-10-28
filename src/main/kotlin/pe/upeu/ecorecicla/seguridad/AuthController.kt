package pe.upeu.ecorecicla.seguridad

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import pe.upeu.ecorecicla.config.JwtService
import pe.upeu.ecorecicla.web.dto.LoginRequest
import pe.upeu.ecorecicla.web.dto.LoginResponse
import pe.upeu.ecorecicla.web.dto.UsuarioResponse

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val usuario = usuarioRepository.findByUsername(request.usuario)
            .orElseThrow { BadCredentialsException("Credenciales incorrectas") }

        if (!usuario.activo || !passwordEncoder.matches(request.contrasena, usuario.passwordHash)) {
            throw BadCredentialsException("Credenciales incorrectas")
        }

        val token = jwtService.generarToken(usuario)
        val privilegios = usuario.obtenerPrivilegios().map { it.name }.toSet()

        return ResponseEntity.ok(
            LoginResponse(
                token = token,
                usuario = usuario.username,
                rol = usuario.rol.name,
                privilegios = privilegios
            )
        )
    }

    @GetMapping("/me")
    fun usuarioActual(@AuthenticationPrincipal usuario: Usuario): ResponseEntity<UsuarioResponse> {
        return ResponseEntity.ok(
            UsuarioResponse(
                id = usuario.id ?: 0L,
                usuario = usuario.username,
                nombreCompleto = usuario.nombreCompleto,
                rol = usuario.rol.name,
                privilegios = usuario.obtenerPrivilegios().map { it.name }.toSet()
            )
        )
    }
}
