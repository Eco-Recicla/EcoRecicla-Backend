package pe.upeu.ecorecicla.dominio

import jakarta.persistence.*
import pe.upeu.ecorecicla.seguridad.Usuario
import java.time.LocalDate
import java.time.LocalDateTime

enum class EstadoRuta {
    ABIERTA,
    CERRADA
}

@Entity
@Table(name = "rutas")
class RutaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var fecha: LocalDate = LocalDate.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var estado: EstadoRuta = EstadoRuta.ABIERTA,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    var usuarioAsignado: Usuario? = null,

    @Column(name = "fecha_cierre")
    var fechaCierre: LocalDateTime? = null,

    @Column(name = "cerrada_por", length = 50)
    var cerradaPor: String? = null
)
