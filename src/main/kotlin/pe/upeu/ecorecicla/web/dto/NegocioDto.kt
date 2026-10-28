package pe.upeu.ecorecicla.web.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class PuntoAcopioDto(
    val id: Long,
    val razonSocial: String,
    val direccion: String,
    val zona: String,
    val materialHabitual: String,
    val secuencia: Int = 0,
    val atendido: Boolean = false
)

data class CrearPuntoAcopioRequest(
    @field:NotBlank(message = "La razón social es obligatoria")
    val razonSocial: String,

    @field:NotBlank(message = "La dirección es obligatoria")
    val direccion: String,

    @field:NotBlank(message = "La zona es obligatoria")
    val zona: String,

    @field:NotBlank(message = "El material habitual es obligatorio")
    val materialHabitual: String
)

data class RutaDto(
    val id: Long,
    val fecha: String,
    val estado: String,
    val usuarioAsignado: String,
    val puntos: List<PuntoAcopioDto>
)

data class AsignarRutaRequest(
    @field:NotBlank(message = "El usuario operario es obligatorio")
    val usuario: String,

    @field:NotNull(message = "La lista de puntos es obligatoria")
    val puntosIds: List<Long>
)

data class PesajeRequest(
    @field:NotNull(message = "El identificador del punto es obligatorio")
    val puntoId: Long?,

    val rutaId: Long? = null,

    @field:NotBlank(message = "El tipo de material es obligatorio")
    val material: String?,

    @field:NotNull(message = "El peso bruto es obligatorio")
    @field:DecimalMin(value = "0.01", message = "El peso bruto debe ser mayor a cero")
    val pesoBruto: Double?,

    @field:NotNull(message = "La tara es obligatoria")
    @field:DecimalMin(value = "0.0", message = "La tara no puede ser negativa")
    val tara: Double?,

    val observacion: String? = null,
    val clientOperationId: String? = null
)

data class PesajeUpdateRequest(
    @field:NotBlank(message = "El tipo de material es obligatorio")
    val material: String?,

    @field:NotNull(message = "El peso bruto es obligatorio")
    @field:DecimalMin(value = "0.01", message = "El peso bruto debe ser mayor a cero")
    val pesoBruto: Double?,

    @field:NotNull(message = "La tara es obligatoria")
    @field:DecimalMin(value = "0.0", message = "La tara no puede ser negativa")
    val tara: Double?,

    val observacion: String? = null
)

data class AnularPesajeRequest(
    @field:NotBlank(message = "El motivo de anulación es obligatorio")
    val motivo: String
)

data class RegistrarOmisionRequest(
    @field:NotBlank(message = "El motivo de omisión es obligatorio")
    val motivo: String
)

data class EstadoPesajeDto(
    val tipo: String,
    val motivo: String? = null
)

data class PesajeDto(
    val id: Long,
    val puntoId: Long,
    val fechaHora: String,
    val material: String,
    val pesoBruto: Double,
    val tara: Double,
    val pesoNeto: Double,
    val operario: String,
    val estado: EstadoPesajeDto,
    val pendiente: Boolean = false
)
