package no.nav.bidrag.automatiskjobb.persistence.repository

import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface RevurderingForskuddRepository : JpaRepository<RevurderingForskudd, Int> {
    @Suppress("Unused")
    fun findAllByStatusIs(
        status: Status,
        pageable: Pageable,
    ): Page<RevurderingForskudd>

    @Suppress("Unused")
    fun findAllByStatusIsAndVedtakIsNotNull(
        status: Status,
        pageable: Pageable,
    ): Page<RevurderingForskudd>

    @Suppress("Unused")
    fun findAllByStatusIsAndBehandlingstypeIsAndOppgaveIsNull(
        status: Status,
        behandlingstype: Behandlingstype,
        pageable: Pageable,
    ): Page<RevurderingForskudd>
}
