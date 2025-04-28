package no.nav.bidrag.automatiskjobb.persistence.repository

import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Status
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AldersjusteringRepository : JpaRepository<Aldersjustering, Int> {
    fun findByBarnIdAndAldersgruppe(
        barnId: Int,
        aldersgruppe: Int,
    ): Aldersjustering?

    @Suppress("Brukes i batch")
    @Query(
        "SELECT a FROM aldersjustering a WHERE a.status = :status",
    )
    fun finnForStatus(
        @Param("status") status: Status,
        pageable: Pageable = Pageable.ofSize(100),
    ): Page<Aldersjustering>

    @Suppress("Brukes i batch")
    @Query(
        "SELECT a FROM aldersjustering a WHERE a.status IN :statuser",
    )
    fun finnForFlereStatuser(
        @Param("statuser") statuser: List<Status>,
        pageable: Pageable = Pageable.ofSize(100),
    ): Page<Aldersjustering>

    fun existsAldersjusteringsByBarnIdAndAldersgruppe(
        barnId: Int,
        aldersgruppe: Int,
    ): Boolean
}
