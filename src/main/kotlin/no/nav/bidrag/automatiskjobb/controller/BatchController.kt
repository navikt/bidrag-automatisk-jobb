package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett.OpprettAldersjusteringerBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.forskudd.AldersjusteringForskuddBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettvedtaksforslag.SlettVedtaksforslagBatch
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.YearMonth

@Protected
@RestController
class BatchController(
    private val slettVedtaksforslagBatch: SlettVedtaksforslagBatch,
    private val opprettAldersjusteringerBidragBatch: OpprettAldersjusteringerBidragBatch,
    private val aldersjusteringForskuddBatch: AldersjusteringForskuddBatch,
) {
    @PostMapping("/aldersjuster/batch/bidrag")
    @Operation(
        summary = "Start kjøring av aldersjustering batch for bidrag.",
        description = "Operasjon for å starte kjøring av aldersjustering batch for bidrag for et gitt år.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Aldersjustering batch ble startet.",
            ),
        ],
    )
    @Parameters(
        value = [
            Parameter(
                name = "forDato",
                description = "År og måned for aldersjustering",
                example = "2025-06",
                required = true,
            ),
            Parameter(
                name = "kjøretidspunkt",
                example = "2025-06-01",
                description =
                    "Kjøretidspunkt for aldersjustering. " +
                        "Default er dagens dato. Kan settes for å justere cutoff dato for opphørte bidrag.",
                required = false,
            ),
            Parameter(
                name = "år",
                example = "2025",
                description =
                    "År det skal aldersjusteres for.",
                required = true,
            ),
            Parameter(
                name = "simuler",
                example = "true",
                description =
                    "Simuleringsmodus for aldersjustering. " +
                        "Default er true.",
                required = false,
            ),
        ],
    )
    fun startAldersjusteringBidragBatch(
        @RequestParam forDato: YearMonth,
        @RequestParam(required = false) kjøretidspunkt: LocalDate?,
        @RequestParam år: Long,
        @RequestParam(required = false) simuler: Boolean = true,
    ): ResponseEntity<Any> {
//        aldersjusteringBidragBatch.startAldersjusteringBatch(forDato, kjøretidspunkt, år, simuler) TODO()
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjuster/batch/slettvedtaksforslag")
    @Operation(
        summary = "Starter batch som sletter vedtaksforslag.",
        description =
            "Operasjon for å starte kjøring av batch som sletter vedtaksforslag som er " +
                "markert med status SLETTES og setter disse til SLETTET.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Aldersjustering batch ble startet.",
            ),
        ],
    )
    fun startSlettVedtaksforslagBatch(): ResponseEntity<Any> {
        slettVedtaksforslagBatch.startSlettVedtaksforslagBatch()
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjuster/batch/forskudd")
    @Operation(
        summary = "Start kjøring av aldersjustering batch for forskudd.",
        description = "Operasjon for å starte kjøring av aldersjustering batch for forskudd for et gitt år.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Aldersjustering batch ble startet.",
            ),
        ],
    )
    @Parameters(
        value = [
            Parameter(
                name = "forDato",
                description = "År og måned for aldersjustering",
                example = "2024-06",
                required = true,
            ),
            Parameter(
                name = "kjøretidspunkt",
                example = "2024-06-01",
                description =
                    "Kjøretidspunkt for aldersjustering. " +
                        "Default er dagens dato. Kan settes for å justere cutoff dato for opphørte forskudd.",
                required = false,
            ),
        ],
    )
    fun startAldersjusteringForskuddBatch(
        @RequestParam forDato: YearMonth,
        @RequestParam(required = false) kjøretidspunkt: LocalDate?,
    ): ResponseEntity<Any> {
        // aldersjusteringForskuddBatch.startAldersjusteringBatch(forDato, kjøretidspunkt)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/aldersjuster/batch/bidrag/opprett")
    @Operation(
        summary = "Start kjøring av batch for å opprette aldersjusteringer.",
        description = "Operasjon for å starte kjøring av batch som oppretter aldersjusteringer for et gitt år.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for oppretting av aldersjusteringer ble startet.",
            ),
        ],
    )
    @Parameters(
        value = [
            Parameter(
                name = "år",
                example = "2025",
                description =
                    "År det skal aldersjusteres for.",
                required = true,
            ),
            Parameter(
                name = "kjøretidspunkt",
                example = "2025-06-01",
                description =
                    "Kjøretidspunkt for aldersjustering. " +
                        "Default er dagens dato. Kan settes for å justere cutoff dato for opphørte bidrag.",
                required = false,
            ),
        ],
    )
    fun startOpprettAldersjusteringBidragBatch(
        @RequestParam år: Long,
        @RequestParam(required = false) kjøretidspunkt: LocalDate?,
    ): ResponseEntity<Any> {
        opprettAldersjusteringerBidragBatch.startOpprettAldersjusteringBidragBatch(
            kjøretidspunkt,
            år,
        )
        return ResponseEntity.ok().build()
    }
}
