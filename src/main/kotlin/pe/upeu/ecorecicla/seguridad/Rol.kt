package pe.upeu.ecorecicla.seguridad

enum class Rol(val descripcion: String, val privilegios: Set<Privilegio>) {
    OPERARIO(
        "Operario de ruta",
        setOf(
            Privilegio.PUNTO_LEER,
            Privilegio.PESAJE_LEER,
            Privilegio.PESAJE_CREAR,
            Privilegio.PESAJE_EDITAR
        )
    ),
    SUPERVISOR(
        "Supervisor de operaciones",
        setOf(
            Privilegio.PUNTO_LEER,
            Privilegio.PESAJE_LEER,
            Privilegio.PESAJE_CREAR,
            Privilegio.PESAJE_EDITAR,
            Privilegio.PESAJE_ANULAR,
            Privilegio.RUTA_CERRAR
        )
    ),
    ADMINISTRADOR(
        "Administrador",
        setOf(
            Privilegio.PUNTO_LEER,
            Privilegio.PESAJE_LEER,
            Privilegio.PUNTO_GESTIONAR,
            Privilegio.USUARIO_GESTIONAR
        )
    ),
    AUDITOR(
        "Auditor de operaciones",
        setOf(
            Privilegio.PUNTO_LEER,
            Privilegio.PESAJE_LEER
        )
    )
}
