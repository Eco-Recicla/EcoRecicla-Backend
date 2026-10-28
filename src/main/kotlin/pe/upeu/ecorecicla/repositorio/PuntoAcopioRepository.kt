package pe.upeu.ecorecicla.repositorio

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.upeu.ecorecicla.dominio.PuntoAcopioEntity

@Repository
interface PuntoAcopioRepository : JpaRepository<PuntoAcopioEntity, Long> {
    fun findByZonaIgnoreCaseAndActivoTrue(zona: String, pageable: Pageable): Page<PuntoAcopioEntity>
    fun findByActivoTrue(pageable: Pageable): Page<PuntoAcopioEntity>
    fun findByZonaIgnoreCase(zona: String, pageable: Pageable): Page<PuntoAcopioEntity>
}
