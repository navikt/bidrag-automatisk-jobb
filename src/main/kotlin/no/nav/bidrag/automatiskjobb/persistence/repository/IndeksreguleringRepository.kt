package no.nav.bidrag.automatiskjobb.persistence.repository

import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface IndeksreguleringRepository : JpaRepository<Indeksregulering, Int> {
    fun findByBarnAndStønadstypeAndÅr(
        barn: Barn,
        stønadstype: Stønadstype,
        år: Int,
    ): Indeksregulering?

    fun findAllByStønadstypeInAndÅr(
        stønadstyper: Collection<Stønadstype>,
        år: Int,
    ): List<Indeksregulering>

    fun findAllByStatusAndBehandlingstypeAndStønadstypeInAndÅr(
        status: Status,
        behandlingstype: Behandlingstype,
        stønadstyper: Collection<Stønadstype>,
        år: Int,
    ): List<Indeksregulering>
}
