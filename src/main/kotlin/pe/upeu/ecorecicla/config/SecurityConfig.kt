package pe.upeu.ecorecicla.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import pe.upeu.ecorecicla.seguridad.Privilegio
import pe.upeu.ecorecicla.web.dto.ErrorRespuesta
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val objectMapper: ObjectMapper
) {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationEntryPoint(): AuthenticationEntryPoint {
        return AuthenticationEntryPoint { request: HttpServletRequest, response: HttpServletResponse, _: AuthenticationException ->
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"
            val error = ErrorRespuesta(
                timestamp = LocalDateTime.now().format(formatter),
                status = HttpStatus.UNAUTHORIZED.value(),
                error = HttpStatus.UNAUTHORIZED.reasonPhrase,
                message = "Token ausente, alterado o vencido",
                path = request.requestURI
            )
            response.writer.write(objectMapper.writeValueAsString(error))
        }
    }

    @Bean
    fun accessDeniedHandler(): AccessDeniedHandler {
        return AccessDeniedHandler { request: HttpServletRequest, response: HttpServletResponse, _: AccessDeniedException ->
            response.status = HttpStatus.FORBIDDEN.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"
            val error = ErrorRespuesta(
                timestamp = LocalDateTime.now().format(formatter),
                status = HttpStatus.FORBIDDEN.value(),
                error = HttpStatus.FORBIDDEN.reasonPhrase,
                message = "Acceso denegado: no posee el privilegio requerido",
                path = request.requestURI
            )
            response.writer.write(objectMapper.writeValueAsString(error))
        }
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint())
                it.accessDeniedHandler(accessDeniedHandler())
            }
            .authorizeHttpRequests { auth ->
                auth
                    // Login público
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                    
                    // Usuario actual
                    .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()

                    // Puntos de acopio: lectura requiere PUNTO_LEER
                    .requestMatchers(HttpMethod.GET, "/api/v1/puntos/**").hasAuthority(Privilegio.PUNTO_LEER.name)
                    // Puntos de acopio: gestión requiere PUNTO_GESTIONAR
                    .requestMatchers(HttpMethod.POST, "/api/v1/puntos/**").hasAuthority(Privilegio.PUNTO_GESTIONAR.name)
                    .requestMatchers(HttpMethod.PUT, "/api/v1/puntos/**").hasAuthority(Privilegio.PUNTO_GESTIONAR.name)

                    // Rutas: consulta requiere PUNTO_LEER
                    .requestMatchers(HttpMethod.GET, "/api/v1/rutas/**").hasAuthority(Privilegio.PUNTO_LEER.name)
                    // Cierre de ruta requiere RUTA_CERRAR (solo supervisor)
                    .requestMatchers(HttpMethod.POST, "/api/v1/rutas/*/cerrar").hasAuthority(Privilegio.RUTA_CERRAR.name)
                    // Registrar omisión: requiere PESAJE_CREAR o PUNTO_LEER/PUNTO_GESTIONAR (documentado: operario y supervisor en ruta)
                    .requestMatchers(HttpMethod.POST, "/api/v1/rutas/*/puntos/*/omision").hasAnyAuthority(
                        Privilegio.PESAJE_CREAR.name,
                        Privilegio.PESAJE_EDITAR.name
                    )

                    // Pesajes: consulta requiere PESAJE_LEER
                    .requestMatchers(HttpMethod.GET, "/api/v1/pesajes/**").hasAuthority(Privilegio.PESAJE_LEER.name)
                    // Registro requiere PESAJE_CREAR (operario y supervisor)
                    .requestMatchers(HttpMethod.POST, "/api/v1/pesajes/**").hasAuthority(Privilegio.PESAJE_CREAR.name)
                    // Edición requiere PESAJE_EDITAR (operario propios del mismo día; supervisor todos)
                    .requestMatchers(HttpMethod.PUT, "/api/v1/pesajes/**").hasAuthority(Privilegio.PESAJE_EDITAR.name)
                    // Anulación requiere PESAJE_ANULAR (solo supervisor)
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/pesajes/**").hasAuthority(Privilegio.PESAJE_ANULAR.name)

                    // Administración de usuarios y padrón
                    .requestMatchers("/api/v1/admin/**").hasAnyAuthority(
                        Privilegio.PUNTO_GESTIONAR.name,
                        Privilegio.USUARIO_GESTIONAR.name
                    )

                    // Todas las demás rutas
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
