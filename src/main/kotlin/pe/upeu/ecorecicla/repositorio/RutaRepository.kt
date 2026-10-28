package pe.upeu.ecorecicla.repositorio

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.upeu.ecorecicla.dominio.RutaEntity
import java.time.LocalDate
import java.util.Optional

@Repository
interface RutaRepository : JpaRepository<RutaEntity, Long> {
    fun findByFechaAndUsuarioAsignadoUsername(fecha: LocalDate, username: String): Optional<RutaEntity>
    fun findByFecha(fecha: LocalDate): List<RutaEntity>
}
