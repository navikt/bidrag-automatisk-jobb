package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.gjennomfor.GjennomførIndeksreguleringBidragBatch
import no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.opprett.OpprettIndeksreguleringBidragBatch
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Year

@Protected
@RestController
@Tag(
    name = "Indeksregulering bidrag batch",
    description = "Endepunkter for å starte og administrere batch-jobber for indeksregulering av bidrag.",
)
class IndeksreguleringBidragBatchController(
    private val opprettIndeksreguleringBidragBatch: OpprettIndeksreguleringBidragBatch,
    private val gjennomførIndeksreguleringBidragBatch: GjennomførIndeksreguleringBidragBatch,
) {
    @PostMapping("/indeksregulering/bidrag/batch/opprett")
    @Operation(
        summary = "Starter batch: Opprett indeksregulering bidrag.",
        description =
            "Starter indeksregulering av bidrag for alle barn med et løpende bidrag, gruppert per sak. " +
                "Batchen kan kjøres flere ganger; saker der indeksreguleringen allerede er opprettet " +
                "eller gjennomført for året hoppes over. Angi valgfritt en liste med saksnummer for å begrense " +
                "kjøringen til kun disse sakene.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Batch for oppretting av indeksregulering av bidrag startet."),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun opprettIndeksreguleringBidrag(
        @RequestParam(required = false) @Parameter(
            required = false,
            description =
                "Valgfri liste med saksnummer som indeksreguleringen skal begrenses til. " +
                    "Behandler alle løpende bidrag hvis tom.",
            example = "2600001",
        ) saksnummer: List<String>?,
        @RequestParam(required = false) @Parameter(
            required = false,
            description = "Året indeksreguleringen gjelder for. Default er inneværende år.",
            example = "2026",
        ) år: Int?,
    ): ResponseEntity<Void> {
        opprettIndeksreguleringBidragBatch.start(saksnummer ?: emptyList(), år ?: Year.now().value)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/indeksregulering/bidrag/batch/gjennomfor")
    @Operation(
        summary = "Starter batch: Gjennomfør indeksregulering bidrag.",
        description =
            "Starter gjennomføring av indeksregulering for alle bidragssaker som er opprettet men ennå ikke " +
                "gjennomført (gjennomfort = false) for det angitte året. Batchen kan kjøres flere ganger; " +
                "allerede gjennomførte saker hoppes over.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Batch for gjennomføring av indeksregulering av bidrag startet."),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun gjennomforIndeksreguleringBidrag(
        @RequestParam(required = false) @Parameter(
            required = false,
            description = "Året indeksreguleringen gjelder for. Default er inneværende år.",
            example = "2026",
        ) år: Int?,
    ): ResponseEntity<Void> {
        gjennomførIndeksreguleringBidragBatch.start(år ?: Year.now().value)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
