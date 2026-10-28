package pe.upeu.ecorecicla.dominio

class ReglaNegocioException(
    override val message: String,
    val codigoEstado: Int = 409
) : RuntimeException(message)

class RecursoNoEncontradoException(
    val recurso: String
) : RuntimeException(recurso)

class ValidacionNegocioException(
    val campo: String,
    override val message: String
) : RuntimeException(message)
