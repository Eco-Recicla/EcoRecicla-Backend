package pe.upeu.ecorecicla.repositorio

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.upeu.ecorecicla.dominio.EstadoPesajeTipo
import pe.upeu.ecorecicla.dominio.PesajeEntity
import java.util.Optional

@Repository
interface PesajeRepository : JpaRepository<PesajeEntity, Long> {
    fun findByPuntoIdOrderByFechaHoraDesc(puntoId: Long, pageable: Pageable): Page<PesajeEntity>
    fun findByRutaId(rutaId: Long): List<PesajeEntity>
    fun findByRutaIdAndPuntoIdAndEstado(rutaId: Long, puntoId: Long, estado: EstadoPesajeTipo): List<PesajeEntity>
    fun findByClientOperationId(clientOperationId: String): Optional<PesajeEntity>
}
