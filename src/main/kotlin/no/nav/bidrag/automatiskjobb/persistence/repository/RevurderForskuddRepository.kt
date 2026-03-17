package no.nav.bidrag.automatiskjobb.persistence.repository

import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface RevurderForskuddRepository : JpaRepository<RevurderingForskudd, Int> {
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
    fun findAllByStatusIsAndVurdereTilbakekrevingIsTrueAndOppgaveIsNull(
        status: Status,
        pageable: Pageable,
    ): Page<RevurderingForskudd>

    @Suppress("Unused")
    fun existsBySaksnummerAndForMåned(
        saksnummer: String,
        måned: String,
    ): Boolean

    @Suppress("Unused")
    fun findAllByForMåned(
        forMåned: String,
        pageable: Pageable,
    ): Page<RevurderingForskudd>

    fun findAllByBehandlingstypeIs(behandlingstype: Behandlingstype): List<RevurderingForskudd>

    @Modifying
    @Query("DELETE FROM revurdering_forskudd r WHERE r.forMåned = :forMåned")
    fun deleteAllByForMåned(forMåned: String): Int
}
