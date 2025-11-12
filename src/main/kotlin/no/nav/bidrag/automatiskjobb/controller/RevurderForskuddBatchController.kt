package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.automatiskjobb.batch.revurderforskudd.beregn.BeregnRevurderForskuddBatch
import no.nav.bidrag.automatiskjobb.batch.revurderforskudd.fattvedtak.FatteVedtakRevurderForskuddBatch
import no.nav.bidrag.automatiskjobb.batch.revurderforskudd.oppgave.OppgaveRevurderForskuddBatch
import no.nav.bidrag.automatiskjobb.batch.revurderforskudd.opprett.OpprettRevurderForskuddBatch
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Tag(name = "Revurder forskudd batch")
class RevurderForskuddBatchController(
    private val opprettRevurderForskuddBatch: OpprettRevurderForskuddBatch,
    private val beregnRevurderForskuddBatch: BeregnRevurderForskuddBatch,
    private val fatteVedtakRevurderForskuddBatch: FatteVedtakRevurderForskuddBatch,
    private val opprettOppgaveRevurderForskuddBatch: OppgaveRevurderForskuddBatch,
) {
    @PostMapping("/revurderforskudd/batch/opprett")
    @Operation(
        summary = "Starter batch: Opprett revurder forskudd.",
        description = "Oppretter revurdering av forskudd for alle barn som ikke har hatt en revurdering siste 12 måneder.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun opprettRevurderForskudd(
        @Parameter(required = false, example = "true") månederTilbakeForManueltVedtak: Int = 12,
    ) {
        opprettRevurderForskuddBatch.start(månederTilbakeForManueltVedtak)
    }

    @PostMapping("/revurderforskudd/batch/beregn")
    @Operation(
        summary = "Starter batch: Beregner revurder forskudd.",
        description = "Beregner revurder av forskudd for alle ubehandlede opprettede revurdering av forskudd og oppretter vedtaksforslag.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun beregnRevurderForskudd(
        @Parameter(required = true, example = "true") simuler: Boolean = true,
        @Parameter(required = true, example = "3") antallMånederForBeregning: Long = 3,
    ) {
        beregnRevurderForskuddBatch.start(simuler, antallMånederForBeregning)
    }

    @PostMapping("/revurderforskudd/batch/fattevedtak")
    @Operation(
        summary = "Starter batch: Fatte vedtak revurder forskudd.",
        description = "Fatter vedtak på revurdering av forskudd for alle beregnede revurderinger.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun fatteVedtakRevurderForskudd(
        @Parameter(required = true, example = "true") simuler: Boolean = true,
    ) {
        fatteVedtakRevurderForskuddBatch.start(simuler)
    }

    @PostMapping("/revurderforskudd/batch/opprettoppgaver")
    @Operation(
        summary = "Starter batch: Opprett oppgaver for revurder forskudd i tilfeller hvor det skal tilbakekreves forskudd.",
        description = "Oppretter oppgaver for saksbehandling i de tilfeller hvor revurdering av forskudd medfører tilbakekreving.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun opprettOppgaverForRevurderForskudd() {
        opprettOppgaveRevurderForskuddBatch.start()
    }
}
