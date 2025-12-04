package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.automatiskjobb.batch.revurderforskudd.evaluer.EvaluerRevurderForskuddBatch
import no.nav.bidrag.automatiskjobb.batch.revurderforskudd.fattvedtak.FatteVedtakRevurderForskuddBatch
import no.nav.bidrag.automatiskjobb.batch.revurderforskudd.oppgave.OppgaveRevurderForskuddBatch
import no.nav.bidrag.automatiskjobb.batch.revurderforskudd.opprett.OpprettRevurderForskuddBatch
import no.nav.bidrag.automatiskjobb.batch.revurderforskudd.revurderingslenke.RevurderingslenkeRevurderForskuddBatch
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@Protected
@RestController
@Tag(name = "Revurder forskudd batch")
class RevurderForskuddBatchController(
    private val opprettRevurderForskuddBatch: OpprettRevurderForskuddBatch,
    private val evaluerRevurderForskuddBatch: EvaluerRevurderForskuddBatch,
    private val fatteVedtakRevurderForskuddBatch: FatteVedtakRevurderForskuddBatch,
    private val opprettOppgaveRevurderForskuddBatch: OppgaveRevurderForskuddBatch,
    private val revurderingslenkeRevurderForskuddBatch: RevurderingslenkeRevurderForskuddBatch,
) {
    @PostMapping("/revurderforskudd/batch/opprett")
    @Operation(
        summary = "Starter batch: Opprett revurder forskudd.",
        description = "Oppretter revurdering av forskudd for alle barn som ikke har hatt en revurdering siste 12 måneder.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun opprettRevurderForskudd(
        @Parameter(required = false, example = "12") månederTilbakeForManueltVedtak: Int = 12,
    ) {
        opprettRevurderForskuddBatch.start(månederTilbakeForManueltVedtak)
    }

    @PostMapping("/revurderforskudd/batch/evaluer")
    @Operation(
        summary = "Starter batch: Evaluering for revurdering av forskudd.",
        description =
            "Evaluerer om forskudd skal revurderes for alle ubehandlede opprettede revurdering av forskudd " +
                "og oppretter vedtaksforslag.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun evaluerRevurderForskudd(
        @Parameter(required = true, example = "true") simuler: Boolean = true,
        @Parameter(required = true, example = "3") antallMånederForBeregning: Long = 3,
        @Parameter(required = false) beregnFraMåned: YearMonth = YearMonth.now().minusYears(1),
    ) {
        evaluerRevurderForskuddBatch.start(simuler, antallMånederForBeregning, beregnFraMåned)
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

    @PostMapping("/revurderforskudd/batch/revurderingslenke")
    @Operation(
        summary = "Starter batch: Opprett revurderingslenke for revurder forskudd i tilfeller hvor det skal tilbakekreves forskudd.",
        description = "Oppretter revurderingslenke for saksbehandling i de tilfeller hvor revurdering av forskudd medfører tilbakekreving.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun opprettRevurderingslengeForRevurderForskudd() {
        revurderingslenkeRevurderForskuddBatch.start()
    }
}
