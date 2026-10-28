package pe.upeu.ecorecicla.seguridad

enum class Privilegio(val descripcion: String) {
    PUNTO_LEER("Consultar la ruta y los puntos de acopio"),
    PESAJE_LEER("Consultar los pesajes de un punto"),
    PESAJE_CREAR("Registrar un pesaje nuevo"),
    PESAJE_EDITAR("Corregir un pesaje existente"),
    PESAJE_ANULAR("Anular un pesaje"),
    RUTA_CERRAR("Cerrar la ruta del día"),
    PUNTO_GESTIONAR("Crear puntos de acopio y asignar rutas"),
    USUARIO_GESTIONAR("Gestionar usuarios y roles")
}
