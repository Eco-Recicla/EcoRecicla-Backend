package pe.upeu.ecorecicla.dominio

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "ruta_puntos",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["ruta_id", "punto_id"]),
        UniqueConstraint(columnNames = ["ruta_id", "secuencia"])
    ]
)
class RutaPuntoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id", nullable = false)
    var ruta: RutaEntity? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "punto_id", nullable = false)
    var punto: PuntoAcopioEntity? = null,

    @Column(nullable = false)
    var secuencia: Int = 1,

    @Column(nullable = false)
    var atendido: Boolean = false,

    @Column(name = "motivo_omision", length = 255)
    var motivoOmision: String? = null,

    @Column(name = "omitido_por", length = 50)
    var omitidoPor: String? = null,

    @Column(name = "fecha_atencion")
    var fechaAtencion: LocalDateTime? = null
)
