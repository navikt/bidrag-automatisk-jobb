package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.bidrag.automatiskjobb.batch.revurderforskudd.evaluer.EvaluerRevurderForskuddBatch
import no.nav.bidrag.automatiskjobb.batch.revurderforskudd.fattvedtak.FatteVedtakRevurderForskuddBatch
import no.nav.bidrag.automatiskjobb.batch.revurderforskudd.oppgave.OppgaveRevurderForskuddBatch
import no.nav.bidrag.automatiskjobb.batch.revurderforskudd.opprett.OpprettRevurderForskuddBatch
import no.nav.bidrag.automatiskjobb.batch.revurderforskudd.revurderingslenke.RevurderingslenkeRevurderForskuddBatch
import no.nav.bidrag.automatiskjobb.service.RevurderForskuddService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
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
    private val revurderForskuddService: RevurderForskuddService,
) {
    @PostMapping("/revurderforskudd/batch/opprett")
    @Operation(
        summary = "Starter batch: Opprett revurder forskudd.",
        description = "Oppretter revurdering av forskudd for alle barn som ikke har hatt en revurdering siste 12 måneder.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Batch for oppretting av revurdering av forskudd startet",
            ),
        ],
    )
    fun opprettRevurderForskudd(
        @Parameter(required = false, example = "12") månederTilbakeForManueltVedtak: Int = 12,
    ): ResponseEntity<Void> {
        opprettRevurderForskuddBatch.start(månederTilbakeForManueltVedtak)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @DeleteMapping("/revurderforskudd/batch/opprett/slett")
    @Operation(
        summary = "Sletter alle revurderinger av forskudd for en måned opprettet av batch.",
        description = "Sletter alle revurderinger av forskudd for en måned opprettet av batch.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun slettRevurderingForskuddForMåned(
        @Parameter(
            required = true,
            description = "Måneden (YYYY-MM) som revurderingene skal slettes for.",
            example = "2026-01",
        ) forMåned: YearMonth,
    ): ResponseEntity<Void> {
        revurderForskuddService.slettRevurderingForskuddForMåned(forMåned)
        return ResponseEntity.status(HttpStatus.OK).build()
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
        @Parameter(
            required = true,
            example = "true",
            description = "Avgjør om batchen skal kjøres i simuleringsmodus.",
        ) simuler: Boolean = true,
        @Parameter(
            required = true,
            example = "3",
            description = "Avgjør hvor mange måneder som skal brukes tilbake i tid for beregning av månedsinntekt.",
        ) antallMånederForBeregning: Long = 3,
        @Parameter(
            required = false,
            description = "Kan settes for å endre hvilken måned beregningen skal gjelde fra. Default er en måned frem i tid.",
        ) beregnFraMåned: YearMonth? = null,
        @Parameter(
            required = false,
            description =
                "Kan settes for å endre hvilken måned av revurdering forskudd innslag som skal behandles. " +
                    "Default er innværende måned.",
        ) forMåned: YearMonth? = null,
    ): ResponseEntity<Void> {
        evaluerRevurderForskuddBatch.start(simuler, antallMånederForBeregning, beregnFraMåned, forMåned)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/revurderforskudd/batch/reskontroVurderTilbakekreving")
    fun reskontroVurderTilbakekreving(): ResponseEntity<Void> {
        CoroutineScope(Dispatchers.IO).launch {
            revurderForskuddService.vurderTilbakekrevingBasertPåReskontro()
        }
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/revurderforskudd/batch/evaluer/resetSimulering")
    @Operation(
        summary = "Resetter evaluering for revurdering av forskudd etter simulering.",
        description =
            "Resetter evaluerer om forskudd skal revurderes så alle innslag er ubehandlede.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun evaluerRevurderForskudd(): ResponseEntity<Void> {
        revurderForskuddService.resetEvalueringEtterSimuering()
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/revurderforskudd/batch/fattevedtak")
    @Operation(
        summary = "Starter batch: Fatte vedtak revurder forskudd.",
        description = "Fatter vedtak på revurdering av forskudd for alle beregnede revurderinger.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun fatteVedtakRevurderForskudd(
        @Parameter(required = true, example = "true") simuler: Boolean = true,
    ): ResponseEntity<Void> {
        fatteVedtakRevurderForskuddBatch.start(simuler)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @Deprecated("Bruk revurderingslenke i stedet")
    @PostMapping("/revurderforskudd/batch/opprettoppgaver")
    @Operation(
        summary = "Starter batch: Opprett oppgaver for revurder forskudd i tilfeller hvor det skal tilbakekreves forskudd.",
        description = "Oppretter oppgaver for saksbehandling i de tilfeller hvor revurdering av forskudd medfører tilbakekreving.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun opprettOppgaverForRevurderForskudd(): ResponseEntity<Void> {
        opprettOppgaveRevurderForskuddBatch.start()
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/revurderforskudd/batch/revurderingslenke")
    @Operation(
        summary = "Starter batch: Opprett revurderingslenke for revurder forskudd i tilfeller hvor det skal tilbakekreves forskudd.",
        description = "Oppretter revurderingslenke for saksbehandling i de tilfeller hvor revurdering av forskudd medfører tilbakekreving.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun opprettRevurderingslengeForRevurderForskudd(): ResponseEntity<Void> {
        revurderingslenkeRevurderForskuddBatch.start()
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
