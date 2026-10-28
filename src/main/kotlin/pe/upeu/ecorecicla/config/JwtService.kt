package pe.upeu.ecorecicla.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pe.upeu.ecorecicla.seguridad.Usuario
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${app.jwt.secret:}") private val secretConfig: String,
    @Value("\${app.jwt.secret-path:./data/jwt.key}") private val secretPath: String,
    @Value("\${app.jwt.expiration-minutes:60}") private val expirationMinutes: Long,
    val clock: Clock = Clock.systemDefaultZone()
) {
    private val signingKey: SecretKey by lazy {
        initOrLoadKey()
    }

    private fun initOrLoadKey(): SecretKey {
        if (secretConfig.isNotBlank() && secretConfig.length >= 32) {
            return Keys.hmacShaKeyFor(secretConfig.toByteArray(StandardCharsets.UTF_8))
        }

        val keyFile = File(secretPath)
        if (keyFile.exists() && keyFile.length() > 0) {
            val encoded = keyFile.readText().trim()
            val decoded = Base64.getDecoder().decode(encoded)
            return Keys.hmacShaKeyFor(decoded)
        }

        keyFile.parentFile?.mkdirs()
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val encoded = Base64.getEncoder().encodeToString(randomBytes)
        keyFile.writeText(encoded)
        return Keys.hmacShaKeyFor(randomBytes)
    }

    fun generarToken(usuario: Usuario): String {
        val now = Instant.now(clock)
        val expiration = now.plus(expirationMinutes, ChronoUnit.MINUTES)

        val privilegios = usuario.obtenerPrivilegios().map { it.name }

        return Jwts.builder()
            .subject(usuario.username)
            .claim("id", usuario.id)
            .claim("rol", usuario.rol.name)
            .claim("privilegios", privilegios)
            .issuedAt(java.util.Date.from(now))
            .expiration(java.util.Date.from(expiration))
            .signWith(signingKey)
            .compact()
    }

    fun validarYExtraerClaims(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(signingKey)
                .clock { java.util.Date.from(Instant.now(clock)) }
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            null
        } catch (e: JwtException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
