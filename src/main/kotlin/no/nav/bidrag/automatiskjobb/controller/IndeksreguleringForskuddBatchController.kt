package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.automatiskjobb.batch.indeksregulering.forskudd.opprett.OpprettIndeksreguleringForskuddBatch
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
    name = "Indeksregulering forskudd batch",
    description = "Endepunkter for å starte og administrere batch-jobber for indeksregulering av forskudd.",
)
class IndeksreguleringForskuddBatchController(
    private val opprettIndeksreguleringForskuddBatch: OpprettIndeksreguleringForskuddBatch,
) {
    @PostMapping("/indeksregulering/forskudd/batch/opprett")
    @Operation(
        summary = "Starter batch: Indeksregulering forskudd.",
        description =
            "Starter indeksregulering av forskudd for alle barn med et løpende forskudd, gruppert per sak. " +
                "Batchen kan kjøres flere ganger uten å utføre indeksreguleringen på nytt; allerede opprettede " +
                "indeksreguleringer for året hoppes over. Angi valgfritt en liste med saksnummer for å begrense " +
                "kjøringen til kun disse sakene.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Batch for indeksregulering av forskudd startet."),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun opprettIndeksreguleringForskudd(
        @RequestParam(required = false) @Parameter(
            required = false,
            description =
                "Valgfri liste med saksnummer som indeksreguleringen skal begrenses til. " +
                    "Behandler alle løpende forskudd hvis tom.",
            example = "2600001",
        ) saksnummer: List<String>?,
        @RequestParam(required = false) @Parameter(
            required = false,
            description = "Året indeksreguleringen gjelder for. Default er inneværende år.",
            example = "2026",
        ) år: Int?,
    ): ResponseEntity<Void> {
        opprettIndeksreguleringForskuddBatch.start(saksnummer ?: emptyList(), år ?: Year.now().value)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
