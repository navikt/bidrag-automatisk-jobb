package no.nav.bidrag.automatiskjobb.persistence.repository

import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Behandlingstype
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

    @Query(
        "SELECT a FROM aldersjustering a WHERE a.status IN :statuser and a.barn.id IN :barnId",
    )
    fun finnForFlereStatuserOgBarnId(
        @Param("statuser") statuser: List<Status>,
        barnId: List<Int>,
    ): List<Aldersjustering>

    @Query(
        "SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM aldersjustering a WHERE a.barn.id = :barn AND a.aldersgruppe = :aldersgruppe",
    )
    fun existsAldersjusteringsByBarnAndAldersgruppe(
        barn: Int,
        aldersgruppe: Int,
    ): Boolean

    @Suppress("unused")
    @Query(
        "SELECT a FROM aldersjustering a " +
            "WHERE (:#{#barnid == null or #barnid.isEmpty()} = true OR a.barn.id IN :barnid) " +
            "AND a.behandlingstype IN :behandlingstyper " +
            "AND a.status IN :statuser " +
            "AND a.oppgave IS NULL",
    )
    fun finnForBarnBehandlingstypeOgStatus(
        @Param("barnid") barn: List<Int>,
        @Param("behandlingstyper") behandlingstyper: List<Behandlingstype>,
        @Param("statuser") statuser: List<Status>,
        pageable: Pageable = Pageable.ofSize(100),
    ): Page<Aldersjustering>

    @Query(
        "SELECT a FROM aldersjustering a " +
            "WHERE (:#{#barnid == null or #barnid.isEmpty()} = true OR a.barn.id IN :barnid) " +
            "AND a.batchId = :batchId " +
            "AND a.oppgave IS NOT NULL",
    )
    fun finnOppgaveOpprettetForBarnOgBatchId(
        barnid: List<Int>,
        batchId: String,
        pageable: Pageable = Pageable.ofSize(100),
    ): Page<Aldersjustering>

    @Query(
        "SELECT a FROM aldersjustering a " +
            "WHERE a.barn.id = :barnid " +
            "AND a.behandlingstype != 'FEILET' " +
            "AND a.status in ('BEHANDLET', 'FATTET')",
    )
    fun finnBarnAldersjustert(
        @Param("barnid") barn: Int,
    ): Aldersjustering?
}
