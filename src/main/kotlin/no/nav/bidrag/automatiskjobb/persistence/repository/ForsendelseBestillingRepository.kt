package no.nav.bidrag.automatiskjobb.persistence.repository

import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ForsendelseBestillingRepository : JpaRepository<ForsendelseBestilling, Int> {
    @Suppress("unused")
    @Query(
        "SELECT f from forsendelseBestilling f where f.slettetTidspunkt is null and " +
            "f.forsendelseId is null or (f.skalSlettes is true and f.distribuertTidspunkt is null and f.journalpostId is null) ",
    )
    fun finnAlleForsendelserSomSkalOpprettesEllerSlettes(
        pageable: Pageable =
            Pageable.ofSize(
                100,
            ),
    ): Page<ForsendelseBestilling>

    @Suppress("unused")
    @Query(
        "SELECT f from forsendelseBestilling f where f.forsendelseId is not null and f.forsendelseOpprettetTidspunkt is not null and f.distribuertTidspunkt is null and f.slettetTidspunkt is null",
    )
    fun finnAlleSomSkalDistribueres(
        pageable: Pageable =
            Pageable.ofSize(
                100,
            ),
    ): Page<ForsendelseBestilling>

    @Suppress("unused")
    @Query(
        "SELECT f from forsendelseBestilling f where f.forsendelseId is not null and f.skalSlettes is true and f.distribuertTidspunkt is null and f.journalpostId is null and f.slettetTidspunkt is null",
    )
    fun finnAlleSomSkalSlettes(
        pageable: Pageable =
            Pageable.ofSize(
                100,
            ),
    ): Page<ForsendelseBestilling>

    @Suppress("unused")
    fun findByAldersjustering(aldersjustering: Aldersjustering): List<ForsendelseBestilling>
}
