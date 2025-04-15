package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.automatiskjobb.batch.bidrag.AldersjusteringBidragBatch
import no.nav.bidrag.automatiskjobb.batch.forskudd.AldersjusteringForskuddBatch
import no.nav.bidrag.automatiskjobb.mapper.VedtakMapper
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringResponse
import no.nav.bidrag.beregn.barnebidrag.service.AldersjusteringOrchestrator
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.security.token.support.core.api.Protected
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

@Protected
@RestController
class AutomatiskJobbController(
    private val aldersjusteringService: AldersjusteringService,
    private val aldersjusteringBidragBatch: AldersjusteringBidragBatch,
    private val aldersjusteringForskuddBatch: AldersjusteringForskuddBatch,
    private val aldersjusteringOrchestrator: AldersjusteringOrchestrator,
    private val vedtakMapper: VedtakMapper,
) {
    @GetMapping("/aldersjustering")
    @Operation(
        summary = "Hent barn som skal aldersjusteres",
        description = "Operasjon for å hente barn som skal aldersjusteres (som fyller 6, 11 eller 15 inneværende år) for et gitt år.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returnerer map over barn som fyller 6, 11 eller 15 for gitt år år.",
            ),
        ],
    )
    fun hentBarnSomSkalAldersjusteres(år: Int): ResponseEntity<Any> =
        ResponseEntity.ok(aldersjusteringService.hentAlleBarnSomSkalAldersjusteresForÅr(år))

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
                example = "2024-06",
                required = true,
            ),
            Parameter(
                name = "kjøretidspunkt",
                example = "2024-06-01",
                description =
                    "Kjøretidspunkt for aldersjustering. " +
                        "Default er dagens dato. Kan settes for å justere cutoff dato for opphørte bidrag.",
                required = false,
            ),
        ],
    )
    fun startAldersjusteringBidragBatch(
        @RequestParam forDato: YearMonth,
        @RequestParam(required = false) kjøretidspunkt: LocalDate?,
    ): ResponseEntity<Any> {
        aldersjusteringBidragBatch.startAldersjusteringBatch(forDato, kjøretidspunkt)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjuster/bidrag/{saksnummer}")
    @Operation(
        summary = "Start kjøring av aldersjustering batch for bidrag.",
        description = "Operasjon for å starte kjøring av aldersjustering batch for bidrag for et gitt år.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun aldersjusterBidragSak(
        @PathVariable saksnummer: Saksnummer,
        @RequestParam(required = false) år: Int?,
        @RequestParam(required = false) simuler: Boolean = true,
    ): AldersjusteringResponse = aldersjusteringService.kjørAldersjusteringForSak(saksnummer, år ?: YearMonth.now().year, simuler)

    @PostMapping("/aldersjuster/bidrag")
    @Operation(
        summary = "Start kjøring av aldersjustering batch for bidrag.",
        description = "Operasjon for å starte kjøring av aldersjustering batch for bidrag for et gitt år.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    suspend fun aldersjusterBidrag(
        @RequestParam(required = false) år: Int?,
        @RequestParam(required = false) simuler: Boolean = true,
        @RequestParam(required = false) batchId: String? = null,
        @RequestParam(required = false) pageSize: Int = 100,
    ): AldersjusteringResponse {
        val kjøringForÅr = år ?: Year.now().value
        return aldersjusteringService.kjørAldersjustering(
            kjøringForÅr,
            batchId ?: "testkjøring_år_$kjøringForÅr",
            simuler,
            if (pageSize == -1) Pageable.unpaged() else Pageable.ofSize(pageSize),
        )
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
        aldersjusteringForskuddBatch.startAldersjusteringBatch(forDato, kjøretidspunkt)
        return ResponseEntity.ok().build()
    }
}
