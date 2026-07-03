package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.gjennomfor.GjennomførIndeksreguleringBidragBatch
import no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.opprett.OpprettIndeksreguleringBidragBatch
import no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.rapporter.RapporterIndeksreguleringBidragBatch
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
    private val rapporterIndeksreguleringBidragBatch: RapporterIndeksreguleringBidragBatch,
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
        @RequestParam(required = false) @Parameter(
            required = false,
            description =
                "Hvis simuler ikke er satt til false så vil det ikke gjennomføres " +
                    "indeksregulering (fattes vedtak), men batchen kjøres uten å utføre endringer.",
            example = "true",
        ) simuler: Boolean = true,
    ): ResponseEntity<Void> {
        gjennomførIndeksreguleringBidragBatch.start(år ?: Year.now().value, simuler)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/indeksregulering/bidrag/batch/rapporter")
    @Operation(
        summary = "Starter batch: Rapporter indeksregulering bidrag.",
        description =
            "Genererer rapportfilene for indeksregulering av bidrag (gjenskaper FB020-rapportstegene fra bisys): " +
                "fil til Bidragsreskontro, filer til FFU for BP i utlandet (brev bestilt, diskresjon, mangler adresse) " +
                "og fil til Elin. Bygger på gjennomførte indeksreguleringer for det angitte året. " +
                "Filene skrives til konfigurert filsti og lastes per nå ikke opp noe sted.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Batch for rapporter for indeksregulering av bidrag startet."),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun rapporterIndeksreguleringBidrag(
        @RequestParam(required = false) @Parameter(
            required = false,
            description = "Året indeksreguleringen gjelder for. Default er inneværende år.",
            example = "2026",
        ) år: Int?,
    ): ResponseEntity<Void> {
        rapporterIndeksreguleringBidragBatch.start(år ?: Year.now().value)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
