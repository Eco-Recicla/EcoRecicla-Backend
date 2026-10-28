package pe.upeu.ecorecicla.dominio

import jakarta.persistence.*
import java.time.LocalDateTime

enum class EstadoPesajeTipo {
    REGISTRADO,
    ANULADO
}

@Entity
@Table(
    name = "pesajes",
    indexes = [
        Index(name = "idx_pesajes_punto", columnList = "punto_id"),
        Index(name = "idx_pesajes_ruta", columnList = "ruta_id"),
        Index(name = "idx_pesajes_client_op", columnList = "client_operation_id", unique = false)
    ]
)
class PesajeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "punto_id", nullable = false)
    var punto: PuntoAcopioEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id", nullable = false)
    var ruta: RutaEntity? = null,

    @Column(name = "fecha_hora", nullable = false)
    var fechaHora: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false, length = 80)
    var material: String = "",

    @Column(name = "peso_bruto", nullable = false)
    var pesoBruto: Double = 0.0,

    @Column(nullable = false)
    var tara: Double = 0.0,

    @Column(name = "peso_neto", nullable = false)
    var pesoNeto: Double = 0.0,

    @Column(nullable = false, length = 50)
    var operario: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var estado: EstadoPesajeTipo = EstadoPesajeTipo.REGISTRADO,

    @Column(name = "motivo_anulacion", length = 255)
    var motivoAnulacion: String? = null,

    @Column(name = "anulado_por", length = 50)
    var anuladoPor: String? = null,

    @Column(name = "fecha_anulacion")
    var fechaAnulacion: LocalDateTime? = null,

    @Column(name = "client_operation_id", length = 100)
    var clientOperationId: String? = null
)
