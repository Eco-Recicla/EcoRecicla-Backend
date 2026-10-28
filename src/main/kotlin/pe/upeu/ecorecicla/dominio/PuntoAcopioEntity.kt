package pe.upeu.ecorecicla.dominio

import jakarta.persistence.*

@Entity
@Table(name = "puntos_acopio")
class PuntoAcopioEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 150)
    var razonSocial: String = "",

    @Column(nullable = false, length = 255)
    var direccion: String = "",

    @Column(nullable = false, length = 80)
    var zona: String = "",

    @Column(nullable = false, length = 80)
    var materialHabitual: String = "",

    @Column(nullable = false)
    var activo: Boolean = true
)
