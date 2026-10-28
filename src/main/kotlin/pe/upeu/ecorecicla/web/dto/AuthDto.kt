package pe.upeu.ecorecicla.web.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "El nombre de usuario es obligatorio")
    val usuario: String,

    @field:NotBlank(message = "La contraseña es obligatoria")
    val contrasena: String
)

data class LoginResponse(
    val token: String,
    val usuario: String,
    val rol: String,
    val privilegios: Set<String>
)

data class UsuarioResponse(
    val id: Long,
    val usuario: String,
    val nombreCompleto: String,
    val rol: String,
    val privilegios: Set<String>
)
