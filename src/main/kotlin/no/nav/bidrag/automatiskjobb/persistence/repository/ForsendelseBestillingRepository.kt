package no.nav.bidrag.automatiskjobb.persistence.repository

import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ForsendelseBestillingRepository : JpaRepository<ForsendelseBestilling, Int> {
    @SuppressWarnings("Brukes i batch")
    fun findAllByForsendelseIdIsNullAndSlettetTidspunktIsNull(
        pageable: Pageable =
            Pageable.ofSize(
                100,
            ),
    ): Page<ForsendelseBestilling>

    @Suppress("Brukes i batch")
    fun findAllByBestiltTidspunktIsNotNullAndForsendelseIdIsNotNullAndSlettetTidspunktIsNull(
        pageable: Pageable =
            Pageable.ofSize(
                100,
            ),
    ): Page<ForsendelseBestilling>
}
