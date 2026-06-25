package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn.BeregnAldersjusteringerBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak.FattVedtakOmAldersjusteringerBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.opprettoppgave.OppgaveAldersjusteringBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.slettoppgave.SlettOppgaveAldersjusteringBidragBatch
import no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett.OpprettAldersjusteringerBidragBatch
import no.nav.bidrag.automatiskjobb.batch.utils.slettallevedtaksforslag.SlettAlleVedtaksforslagBatch
import no.nav.bidrag.automatiskjobb.batch.utils.slettvedtaksforslag.SlettVedtaksforslagBatch
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Protected
@RestController
@Tag(name = "Aldersjustering bidrag batch")
class AldersjusteringBidragBatchController(
    private val slettVedtaksforslagBatch: SlettVedtaksforslagBatch,
    private val slettVedtaksforslagAlleBatch: SlettAlleVedtaksforslagBatch,
    private val opprettAldersjusteringerBidragBatch: OpprettAldersjusteringerBidragBatch,
    private val fattVedtakOmAldersjusteringerBidragBatch: FattVedtakOmAldersjusteringerBidragBatch,
    private val oppgaveAldersjusteringBidragBatch: OppgaveAldersjusteringBidragBatch,
    private val slettOppgaveAldersjusteringBidragBatch: SlettOppgaveAldersjusteringBidragBatch,
    private val beregnAldersjusteringerBidragBatch: BeregnAldersjusteringerBidragBatch,
) {
    @PostMapping("/aldersjustering/batch/slettvedtaksforslag")
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
    @Parameters(
        value = [
            Parameter(
                name = "inkluderBehandlet",
                example = "false",
                description =
                    "Sletter også behandlet aldersjusteringer som ikke er fattet",
                required = false,
            ),
            Parameter(
                name = "barn",
                example = "1,2,3",
                description =
                    "Liste over barn som det skal slettes vedtaksforslag for. Om ingen er sendt kjøres alle. Maks lengde på input er 250 tegn!",
                required = false,
            ),
        ],
    )
    fun startSlettVedtaksforslagBatch(
        @RequestParam inkluderBehandlet: Boolean = false,
        @RequestParam barn: String? = null,
    ): ResponseEntity<Any> {
        slettVedtaksforslagBatch.startSlettVedtaksforslagBatch(inkluderBehandlet, barn)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjustering/batch/slettvedtaksforslag/alle")
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

    @PostMapping("/aldersjustering/batch/bidrag/opprett")
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

    @PostMapping("/aldersjustering/batch/bidrag/fattVedtak")
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
            Parameter(
                name = "behandlingstyper",
                example = "MANUELL,FATTET_FORSLAG,INGEN",
                description =
                    "Liste over behandlingstyper som skal fattes vedtak for.",
                required = false,
            ),
            Parameter(
                name = "kunRedusertBidrag",
                example = "false",
                description =
                    "Når true fattes vedtak kun for aldersjusteringer der aldersjustert beløp er lavere enn løpende beløp.",
                required = false,
            ),
        ],
    )
    fun startFattVedtakAldersjusteringBidragBatch(
        @RequestParam barn: String?,
        @RequestParam simuler: Boolean = true,
        @RequestParam behandlingstyper: List<Behandlingstype> =
            listOf(Behandlingstype.MANUELL, Behandlingstype.FATTET_FORSLAG, Behandlingstype.INGEN),
        @RequestParam kunRedusertBidrag: Boolean = false,
    ): ResponseEntity<Any> {
        fattVedtakOmAldersjusteringerBidragBatch.startFattVedtakOmAldersjusteringBidragBatch(
            barn,
            simuler,
            behandlingstyper,
            kunRedusertBidrag,
        )
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjustering/batch/bidrag/beregn")
    @Operation(
        summary = "Start kjøring av batch for å beregne aldersjusteringer.",
        description =
            "Operasjon for å starte kjøring av batch som beregner aldersjusteringer og oppretter vedtaksforslag. " +
                "Det opprettes også vedtaksforslag for saker som ikke aldersjusteres, med beslutningstype=AVVIST. " +
                "Standard kjøring inkluderer statuser UBEHANDLET, FEILET og SIMULERT.",
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
                name = "statuser",
                example = "UBEHANDLET,FEILET,SIMULERT",
                description =
                    "Kommaseparert liste over statuser som skal inkluderes i kjøringen. " +
                        "Default er UBEHANDLET,FEILET,SIMULERT. " +
                        "Mulige verdier:\n" +
                        "- UBEHANDLET: Nye aldersjusteringer som ikke er beregnet ennå.\n" +
                        "- FEILET: Aldersjusteringer der beregningen feilet og må kjøres på nytt.\n" +
                        "- SIMULERT: Aldersjusteringer som er beregnet i simuleringsmodus og ikke har vedtaksforslag.\n" +
                        "- BEHANDLET: Aldersjusteringer som allerede er beregnet og har vedtaksforslag. " +
                        "Eksisterende vedtaksforslag slettes og det opprettes nytt. Påvirker ikke fattet vedtak.\n" +
                        "- FATTE_VEDTAK_FEILET: Aldersjusteringer der et tidligere forsøk på å fatte vedtak feilet. " +
                        "Nyttig for å rekjøre uten å inkludere alle behandlede poster.\n" +
                        "- SLETTET: Aldersjusteringer som er slettet og skal beregnes på nytt.",
                required = false,
            ),
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
                    "Liste over barn som det skal beregnes aldersjusteringer for. Om ingen er sendt kjøres alle. Maks lengde på input er 250 tegn!",
                required = false,
            ),
        ],
    )
    fun startBeregnAldersjusteringBidragBatch(
        @RequestParam statuser: List<Status> = listOf(Status.UBEHANDLET, Status.FEILET, Status.SIMULERT),
        @RequestParam simuler: Boolean = true,
        @RequestParam barn: String? = null,
    ): ResponseEntity<Any> {
        beregnAldersjusteringerBidragBatch.startBeregnAldersjusteringBidragBatch(
            simuler,
            statuser,
            barn,
        )
        return ResponseEntity.ok().build()
    }

    @PostMapping("/aldersjustering/batch/bidrag/oppgave")
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

    @PostMapping("/aldersjustering/batch/bidrag/oppgave/slett")
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
}
