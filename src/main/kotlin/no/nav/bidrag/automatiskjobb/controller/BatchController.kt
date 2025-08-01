package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn.BeregnAldersjusteringerBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak.FattVedtakOmAldersjusteringerBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.forsendelse.distribuer.DistribuerForsendelseAldersjusteringerBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.forsendelse.opprett.OpprettForsendelseAldersjusteringerBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.forsendelse.slett.SlettForsendelsrSomSkalSlettesBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.opprettoppgave.OppgaveAldersjusteringBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.slettoppgave.SlettOppgaveAldersjusteringBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett.OpprettAldersjusteringerBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettallevedtaksforslag.SlettAlleVedtaksforslagBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.slettvedtaksforslag.SlettVedtaksforslagBatch
import no.nav.bidrag.automatiskjobb.batch.generelt.oppdaterbarn.OppdaterBarnBatch
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
    private val slettOppgaveAldersjusteringBidragBatch: SlettOppgaveAldersjusteringBidragBatch,
    private val beregnAldersjusteringerBidragBatch: BeregnAldersjusteringerBidragBatch,
    private val opprettForsendelseAldersjusteringerBidragBatch: OpprettForsendelseAldersjusteringerBidragBatch,
    private val distribuerForsendelseAldersjusteringerBidragBatch: DistribuerForsendelseAldersjusteringerBidragBatch,
    private val slettForsendelsrSomSkalSlettesBidragBatch: SlettForsendelsrSomSkalSlettesBidragBatch,
    private val oppdaterBarnBatch: OppdaterBarnBatch,
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
                name = "aldersjusteringsdato",
                example = "2025-07-01",
                description =
                    "Kjøretidspunkt for aldersjustering. " +
                        "Default er 1. juli inneværende år. Kan settes for å justere cutoff dato for opphørte bidrag.",
                required = false,
            ),
        ],
    )
    fun startOpprettAldersjusteringBidragBatch(
        @RequestParam(required = true, name = "aar") år: Long,
        @RequestParam(required = false) aldersjusteringsdato: LocalDate?,
    ): ResponseEntity<Any> {
        opprettAldersjusteringerBidragBatch.startOpprettAldersjusteringBidragBatch(
            aldersjusteringsdato,
            år,
        )
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjuster/batch/bidrag/fattVedtak")
    @Operation(
        summary = "Start kjøring av batch for å fatte vedtak om aldersjusteringer.",
        description =
            "Operasjon for å starte kjøring av batch som fatter vedtak om aldersjusteringer. " +
                "Fatter vedtak for alle aldersjusteringer som det er opprettet vedtakforslag på. ",
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
                name = "barn",
                example = "1,2,3",
                description =
                    "Liste over barn som det skal fattes vedtak for. Om ingen er sendt kjøres alle. Maks lengde på input er 250!",
                required = false,
            ),
            Parameter(
                name = "simuler",
                example = "false",
                description =
                    "Hvis simuler ikke er satt til false så vil det " +
                        "ikke fattes vedtak men det vil opprettes forsendelse bestilling",
                required = false,
            ),
        ],
    )
    fun startFattVedtakAldersjusteringBidragBatch(
        @RequestParam barn: String?,
        @RequestParam simuler: Boolean = true,
    ): ResponseEntity<Any> {
        fattVedtakOmAldersjusteringerBidragBatch.startFattVedtakOmAldersjusteringBidragBatch(
            barn,
            simuler,
        )
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjuster/batch/bidrag/beregn")
    @Operation(
        summary = "Start kjøring av batch for å beregne aldersjusteringer.",
        description =
            "Operasjon for å starte kjøring av batch som beregner aldersjusteringer og oppretter vedtaksforslag. " +
                "Det opprettes også vedtaksforslag for saker som også ikke aldersjusteres med beslutningstype=AVVIST",
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
            Parameter(
                name = "barn",
                example = "1,2,3",
                description =
                    "Liste over barn som det skal opprettes oppgaver for. Om ingen er sendt kjøres alle. Maks lengde på input er 250 tegn!",
                required = false,
            ),
        ],
    )
    fun startBeregnAldersjusteringBidragBatch(
        @RequestParam simuler: Boolean = true,
        @RequestParam barn: String? = null,
    ): ResponseEntity<Any> {
        beregnAldersjusteringerBidragBatch.startBeregnAldersjusteringBidragBatch(
            simuler,
            barn,
        )
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjuster/batch/bidrag/oppgave")
    @Operation(
        summary = "Start kjøring av batch for å opprette oppgaver for manuelle aldersjusteringer.",
        description = "Operasjon for å starte kjøring av batch som oppretter oppgaver for aldersjusteringer som skal behandles manuelt.",
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
                name = "barn",
                example = "1,2,3",
                description =
                    "Liste over barn som det skal opprettes oppgaver for. Om ingen er sendt kjøres alle. Maks lengde på input er 250 tegn!",
                required = false,
            ),
        ],
    )
    fun startOppgaveAldersjusteringBidragBatch(
        @RequestParam barn: String?,
    ): ResponseEntity<Any> {
        oppgaveAldersjusteringBidragBatch.startOppgaveAldersjusteringBidragBatch(
            barn,
        )
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjuster/batch/bidrag/oppgave/slett")
    @Operation(
        summary = "Start kjøring av batch for å slette oppgaver for manuelle aldersjusteringer.",
        description = "Operasjon for å starte kjøring av batch som sletter oppgaver for manuelle aldersjusteriner.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for sletter av oppgaver for aldersjusteringer ble startet.",
            ),
        ],
    )
    @Parameters(
        value = [
            Parameter(
                name = "barn",
                example = "1,2,3",
                description =
                    "Liste over barn som det skal opprettes oppgaver for. Om ingen er sendt kjøres alle. Maks lengde på input er 250 tegn!",
                required = false,
            ),
            Parameter(
                name = "batchId",
                example = "XXXYYY",
                description =
                    "BatchId det skal slettes for",
                required = false,
            ),
        ],
    )
    fun startSlettOppgaveAldersjusteringBidragBatch(
        @RequestParam barn: String?,
        @RequestParam batchId: String,
    ): ResponseEntity<Any> {
        slettOppgaveAldersjusteringBidragBatch.startSlettOppgaveAldersjusteringBidragBatch(
            barn,
            batchId,
        )
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjuster/batch/forsendelse/opprett")
    @Operation(
        summary = "Start kjøring av batch for å opprette forsendelse for aldersjusteringer.",
        description = "Operasjon for å starte kjøring av batch som oppretter forsendelse for aldersjusteringer.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for oppretting av forsendelser for aldersjusteringer ble startet.",
            ),
        ],
    )
    fun startOpprettForsendelseAldersjusteringBidragBatch(
        @RequestParam prosesserFeilet: Boolean = false,
    ): ResponseEntity<Any> {
        opprettForsendelseAldersjusteringerBidragBatch.startOpprettForsendelseAldersjusteringBidragBatch(prosesserFeilet)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/batch/forsendelse/slett")
    @Operation(
        summary = "Start kjøring av batch som sletter alle forsendelser som har blitt satt til skalSlettes=true.",
        description =
            "Operasjon for å starte kjøring av batch som skal slette alle forsendelser " +
                "som er satt til skal slettes med kolonnen skal_slettes=true",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for sletting av forsendelser ble startet",
            ),
        ],
    )
    fun starSlettForsendelserSomSkalSlettesBatch(): ResponseEntity<Any> {
        slettForsendelsrSomSkalSlettesBidragBatch.startSlettForsendelserSomSkalSlettesBatch()
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjuster/batch/forsendelse/distribuer")
    @Operation(
        summary = "Start kjøring av batch for å distribuere forsendelse for aldersjusteringer.",
        description = "Operasjon for å starte kjøring av batch som distribuerer forsendelser for aldersjusteringer.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for distrubisjon av forsendelser for aldersjusteringer ble startet.",
            ),
        ],
    )
    fun startDistribuerForsendelseAldersjusteringBidragBatch(): ResponseEntity<Any> {
        distribuerForsendelseAldersjusteringerBidragBatch.startDistribuerForsendelseAldersjusteringBidragBatch()
        return ResponseEntity.ok().build()
    }

    @PostMapping("/barn/oppdater")
    @Operation(
        summary = "Start kjøring av batch for å oppdatere barn med perioder for forskudd og bidrag",
        description =
            "Operasjon for å starte kjøring av batch som oppdaterer " +
                "barn med perioder for forskudd og bidrag ved å hente data fra bidrag-beløpshistorikk",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for oppdatering av barn perioder startet",
            ),
        ],
    )
    @Parameters(
        value = [
            Parameter(
                name = "simuler",
                example = "true",
                description = "Simuleringsmodus. Default er true.",
                required = false,
            ),
            Parameter(
                name = "barn",
                example = "1,2,3",
                description =
                    "Liste over barn som det skal oppdateres for",
                required = false,
            ),
        ],
    )
    fun startOppdaterBarnBatch(
        @RequestParam simuler: Boolean = true,
        @RequestParam barn: String? = null,
        @RequestParam dagerTilbake: Int? = null,
    ): ResponseEntity<Any> {
        oppdaterBarnBatch.startOppdaterBarnBatch(barn, dagerTilbake ?: 1, simuler)
        return ResponseEntity.ok().build()
    }
}
