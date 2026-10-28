package pe.upeu.ecorecicla.seguridad

import jakarta.persistence.*

@Entity
@Table(name = "usuarios")
class Usuario(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 50)
    var username: String = "",

    @Column(nullable = false, length = 200)
    var passwordHash: String = "",

    @Column(nullable = false, length = 100)
    var nombreCompleto: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var rol: Rol = Rol.OPERARIO,

    @Column(nullable = false)
    var activo: Boolean = true
) {
    fun obtenerPrivilegios(): Set<Privilegio> {
        return rol.privilegios
    }
}
