package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn.BeregnAldersjusteringerBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.brev.distribuer.DistribuerBrevAldersjusteringerBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.brev.opprett.OpprettBrevAldersjusteringerBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak.FattVedtakOmAldersjusteringerBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.OppgaveAldersjusteringBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett.OpprettAldersjusteringerBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.forskudd.AldersjusteringForskuddBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettallevedtaksforslag.SlettAlleVedtaksforslagBatch
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
    private val slettVedtaksforslagAlleBatch: SlettAlleVedtaksforslagBatch,
    private val opprettAldersjusteringerBidragBatch: OpprettAldersjusteringerBidragBatch,
    private val fattVedtakOmAldersjusteringerBidragBatch: FattVedtakOmAldersjusteringerBidragBatch,
    private val oppgaveAldersjusteringBidragBatch: OppgaveAldersjusteringBidragBatch,
    private val aldersjusteringForskuddBatch: AldersjusteringForskuddBatch,
    private val beregnAldersjusteringerBidragBatch: BeregnAldersjusteringerBidragBatch,
    private val opprettBrevAldersjusteringerBidragBatch: OpprettBrevAldersjusteringerBidragBatch,
    private val distribuerBrevAldersjusteringerBidragBatch: DistribuerBrevAldersjusteringerBidragBatch,
) {
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

    @PostMapping("/aldersjuster/batch/slettvedtaksforslag/alle")
    @Operation(
        summary = "Starter batch som sletter alle eksisterende vedtaksforslag.",
        description =
            "Operasjon for å starte kjøring av batch som sletter alle vedtaksforslag som fra bidrag-vedtak",
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
    fun startSlettAlleedtaksforslagBatch(): ResponseEntity<Any> {
        slettVedtaksforslagAlleBatch.startAlleSlettVedtaksforslagBatch()
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
                name = "aar",
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
        @RequestParam(required = true, name = "aar") år: Long,
        @RequestParam(required = false) kjøretidspunkt: LocalDate?,
    ): ResponseEntity<Any> {
        opprettAldersjusteringerBidragBatch.startOpprettAldersjusteringBidragBatch(
            kjøretidspunkt,
            år,
        )
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjuster/batch/bidrag/fattVedtak")
    @Operation(
        summary = "Start kjøring av batch for å fatte vedtak om aldersjusteringer.",
        description =
            "Operasjon for å starte kjøring av batch som fatter vedtak om aldersjusteringer. " +
                "Fatter vedtak for alle aldersjusteringer som det er sendt vedtakforslag på. ",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for fatting av vedtak om aldersjusteringer ble startet.",
            ),
        ],
    )
    @Parameters(
        value = [
            Parameter(
                name = "barnId",
                example = "1,2,3",
                description =
                    "Liste over barn som det skal fattes vedtak for. Om ingen er sendt kjøres alle. Maks lengde på input er 250!",
                required = false,
            ),
        ],
    )
    fun startFattVedtakAldersjusteringBidragBatch(
        @RequestParam barn: String,
    ): ResponseEntity<Any> {
        fattVedtakOmAldersjusteringerBidragBatch.startFattVedtakOmAldersjusteringBidragBatch(
            barn,
        )
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjuster/batch/bidrag/beregn")
    @Operation(
        summary = "Start kjøring av batch for å beregne aldersjusteringer.",
        description =
            "Operasjon for å starte kjøring av batch som beregner aldersjusteringer og sender vedtaksforslag. ",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for beregning av aldersjusteringer ble startet.",
            ),
        ],
    )
    @Parameters(
        value = [
            Parameter(
                name = "simuler",
                example = "true",
                description = "Simuleringsmodus for aldersjustering. Default er true.",
                required = false,
            ),
        ],
    )
    fun startBeregnAldersjusteringBidragBatch(
        @RequestParam simuler: Boolean = true,
    ): ResponseEntity<Any> {
        beregnAldersjusteringerBidragBatch.startBeregnAldersjusteringBidragBatch(
            simuler,
        )
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjuster/batch/bidrag/oppgave")
    @Operation(
        summary = "Start kjøring av batch for å opprette oppgaver for manuelle aldersjusteringer.",
        description = "Operasjon for å starte kjøring av batch som oppretter oppgaver for manuelle aldersjusteriner.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for oppretting av oppgaver for aldersjusteringer ble startet.",
            ),
        ],
    )
    @Parameters(
        value = [
            Parameter(
                name = "barnId",
                example = "1,2,3",
                description =
                    "Liste over barn som det skal opprettes oppgaver for. Om ingen er sendt kjøres alle. Maks lengde på input er 250 tegn!",
                required = false,
            ),
        ],
    )
    fun startOppgaveAldersjusteringBidragBatch(
        @RequestParam barn: String,
    ): ResponseEntity<Any> {
        oppgaveAldersjusteringBidragBatch.startOppgaveAldersjusteringBidragBatch(
            barn,
        )
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjuster/batch/brev/opprett")
    @Operation(
        summary = "Start kjøring av batch for å opprette brev for aldersjusteringer.",
        description = "Operasjon for å starte kjøring av batch som oppretter brev for aldersjusteriner.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for oppretting av brev for aldersjusteringer ble startet.",
            ),
        ],
    )
    fun startOpprettBrevAldersjusteringBidragBatch(): ResponseEntity<Any> {
        opprettBrevAldersjusteringerBidragBatch.startOpprettBrevAldersjusteringBidragBatch()
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjuster/batch/brev/distribuer")
    @Operation(
        summary = "Start kjøring av batch for å distribuere brev for aldersjusteringer.",
        description = "Operasjon for å starte kjøring av batch som distibuerer brew for aldersjusteriner.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for distrubisjon av breev for aldersjusteringer ble startet.",
            ),
        ],
    )
    fun startDistribuerBrevAldersjusteringBidragBatch(): ResponseEntity<Any> {
        distribuerBrevAldersjusteringerBidragBatch.startDistribuerBrevAldersjusteringBidragBatch()
        return ResponseEntity.ok().build()
    }
}
