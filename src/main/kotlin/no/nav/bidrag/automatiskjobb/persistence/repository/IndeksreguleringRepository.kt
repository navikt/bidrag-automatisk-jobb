package no.nav.bidrag.automatiskjobb.persistence.repository

import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface IndeksreguleringRepository : JpaRepository<Indeksregulering, Int> {
    @Suppress("Unused")
    fun findAllByStatusIs(
        status: Status,
        pageable: Pageable,
    ): Page<Indeksregulering>

    fun findByBarnAndStønadstypeAndÅr(
        barn: Barn,
        stønadstype: Stønadstype,
        år: Int,
    ): Indeksregulering?

    fun findAllByGjennomfortFalseAndStønadstypeAndÅr(
        stønadstype: Stønadstype,
        år: Int,
        pageable: Pageable,
    ): Page<Indeksregulering>

    fun findAllByGjennomfortTrueAndStønadstypeAndÅr(
        stønadstype: Stønadstype,
        år: Int,
    ): List<Indeksregulering>
}
