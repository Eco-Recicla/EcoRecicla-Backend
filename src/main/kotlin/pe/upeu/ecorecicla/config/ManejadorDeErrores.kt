package pe.upeu.ecorecicla.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import pe.upeu.ecorecicla.dominio.RecursoNoEncontradoException
import pe.upeu.ecorecicla.dominio.ReglaNegocioException
import pe.upeu.ecorecicla.dominio.ValidacionNegocioException
import pe.upeu.ecorecicla.web.dto.ErrorRespuesta
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestControllerAdvice
class ManejadorDeErrores {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    private fun ahora(): String = LocalDateTime.now().format(formatter)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun manejarValidacion(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorRespuesta> {
        val errores = mutableMapOf<String, String>()
        for (error in ex.bindingResult.allErrors) {
            val campo = (error as? FieldError)?.field ?: error.objectName
            val mensaje = error.defaultMessage ?: "Valor inválido"
            errores[campo] = mensaje
        }

        val respuesta = ErrorRespuesta(
            timestamp = ahora(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Datos inválidos en la solicitud",
            path = request.requestURI,
            validationErrors = errores
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta)
    }

    @ExceptionHandler(ValidacionNegocioException::class)
    fun manejarValidacionNegocio(
        ex: ValidacionNegocioException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorRespuesta> {
        val respuesta = ErrorRespuesta(
            timestamp = ahora(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = ex.message,
            path = request.requestURI,
            validationErrors = mapOf(ex.campo to ex.message)
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta)
    }

    @ExceptionHandler(ReglaNegocioException::class)
    fun manejarReglaNegocio(
        ex: ReglaNegocioException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorRespuesta> {
        val status = HttpStatus.resolve(ex.codigoEstado) ?: HttpStatus.CONFLICT
        val respuesta = ErrorRespuesta(
            timestamp = ahora(),
            status = status.value(),
            error = status.reasonPhrase,
            message = ex.message,
            path = request.requestURI
        )
        return ResponseEntity.status(status).body(respuesta)
    }

    @ExceptionHandler(RecursoNoEncontradoException::class)
    fun manejarNoEncontrado(
        ex: RecursoNoEncontradoException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorRespuesta> {
        val respuesta = ErrorRespuesta(
            timestamp = ahora(),
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = ex.recurso,
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun manejarCredencialesIncorrectas(
        ex: BadCredentialsException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorRespuesta> {
        val respuesta = ErrorRespuesta(
            timestamp = ahora(),
            status = HttpStatus.UNAUTHORIZED.value(),
            error = HttpStatus.UNAUTHORIZED.reasonPhrase,
            message = "Credenciales incorrectas",
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun manejarAccesoDenegado(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorRespuesta> {
        val respuesta = ErrorRespuesta(
            timestamp = ahora(),
            status = HttpStatus.FORBIDDEN.value(),
            error = HttpStatus.FORBIDDEN.reasonPhrase,
            message = "Acceso denegado: no posee el privilegio requerido",
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(respuesta)
    }

    @ExceptionHandler(Exception::class)
    fun manejarErrorGeneral(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorRespuesta> {
        val respuesta = ErrorRespuesta(
            timestamp = ahora(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            message = "Error interno del servidor",
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta)
    }
}
