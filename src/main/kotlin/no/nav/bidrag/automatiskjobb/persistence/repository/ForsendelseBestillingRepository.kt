package no.nav.bidrag.automatiskjobb.persistence.repository

import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import org.springframework.data.jpa.repository.JpaRepository

interface ForsendelseBestillingRepository : JpaRepository<ForsendelseBestilling, Int> {
    fun findAllByForsendelseIdIsNullAndSlettetTidspunktIsNull(): List<ForsendelseBestilling>

    fun findAllByBestiltTidspunktIsNotNullAndForsendelseIdIsNotNullAndSlettetTidspunktIsNull(): List<ForsendelseBestilling>
}
