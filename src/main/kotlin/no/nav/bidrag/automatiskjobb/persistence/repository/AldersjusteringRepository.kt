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
        "SELECT a FROM aldersjustering a WHERE a.status IN :statuser and a.barnId IN :barnId",
    )
    fun finnForFlereStatuserOgBarnId(
        @Param("statuser") statuser: List<Status>,
        barnId: List<Int>,
    ): List<Aldersjustering>

    fun existsAldersjusteringsByBarnIdAndAldersgruppe(
        barnId: Int,
        aldersgruppe: Int,
    ): Boolean

    @Suppress("Brukes i batch")
    @Query(
        "SELECT a FROM aldersjustering a " +
            "WHERE a.behandlingstype IN :behandlingstyper " +
            "AND a.status IN :statuser " +
            "AND a.oppgave IS NULL",
    )
    fun finnForBehandlingstypeOgStatus(
        @Param("behandlingstype") behandlingstyper: List<Behandlingstype>,
        @Param("status") statuser: List<Status>,
        pageable: Pageable = Pageable.ofSize(100),
    ): Page<Aldersjustering>

    @Suppress("Brukes i batch")
    @Query(
        "SELECT a FROM aldersjustering a " +
            "WHERE a.barnId in :barnid " +
            "AND a.behandlingstype IN :behandlingstyper " +
            "AND a.status IN :statuser " +
            "AND a.oppgave IS NULL",
    )
    fun finnForBarnBehandlingstypeOgStatus(
        @Param("barnid") barn: List<Int>,
        @Param("behandlingstype") behandlingstyper: List<Behandlingstype>,
        @Param("status") statuser: List<Status>,
        pageable: Pageable = Pageable.ofSize(100),
    ): Page<Aldersjustering>

    @Suppress("Brukes i batch")
    fun findAllByVedtakjournalpostIdIsNull(): List<Aldersjustering>

    @Suppress("Brukes i batch")
    fun findAllByVedtakforselselseIdIsNotNullAndVedtakjournalpostIdIsNull(): List<Aldersjustering>
}
