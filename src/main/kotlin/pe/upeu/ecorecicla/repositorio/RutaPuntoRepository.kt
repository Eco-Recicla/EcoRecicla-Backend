package pe.upeu.ecorecicla.repositorio

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.upeu.ecorecicla.dominio.RutaPuntoEntity
import java.util.Optional

@Repository
interface RutaPuntoRepository : JpaRepository<RutaPuntoEntity, Long> {
    fun findByRutaIdOrderBySecuenciaAsc(rutaId: Long): List<RutaPuntoEntity>
    fun findByRutaIdAndPuntoId(rutaId: Long, puntoId: Long): Optional<RutaPuntoEntity>
}
