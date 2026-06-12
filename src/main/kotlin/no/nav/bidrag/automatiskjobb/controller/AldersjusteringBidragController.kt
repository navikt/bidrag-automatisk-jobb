package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringResponse
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.automatiskjobb.AldersjusteringResultatlisteResponse
import no.nav.bidrag.transport.automatiskjobb.HentAldersjusteringStatusRequest
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@Protected
@RestController
@Tag(name = "Aldersjustering Bidrag")
class AldersjusteringBidragController(
    private val aldersjusteringService: AldersjusteringService,
) {
    @GetMapping("/aldersjustering/{id}")
    @Operation(
        summary = "Henter innslag fra aldersjusteringstabellen",
        description = "Operasjon for å hente innslag i aldersjusteringstabellen.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hentet aldersjustering.",
                content = [Content(schema = Schema(implementation = Aldersjustering::class))],
            ),
            ApiResponse(responseCode = "204", description = "Fant ingen aldersjustering for oppgitt id."),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun hentAldersjustering(
        @RequestParam id: Int,
    ): ResponseEntity<Any> {
        val aldersjustering =
            aldersjusteringService.hentAldersjustering(id) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(aldersjustering)
    }

    @PostMapping("/aldersjustering")
    @Operation(
        summary = "Legger til innslag i aldersjusteringstabellen",
        description = "Operasjon for å legge til nye innslag i aldersjusteringstabellen.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Opprettet aldersjustering. Returnerer det lagrede aldersjusteringsinnslagget.",
                content = [Content(schema = Schema(implementation = Aldersjustering::class))],
            ),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun lagreAldersjustering(
        @RequestBody aldersjustering: Aldersjustering,
    ): ResponseEntity<Any> = ResponseEntity.ok(aldersjusteringService.lagreAldersjustering(aldersjustering))

    @GetMapping("/aldersjustering/bidrag/barn")
    @Operation(
        summary = "Hent barn som skal aldersjusteres",
        description = "Operasjon for å hente barn som skal aldersjusteres (som fyller 6, 11 eller 15 inneværende år) for et gitt år.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returnerer map over barn som fyller 6, 11 eller 15 for gitt år.",
            ),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun hentBarnSomSkalAldersjusteres(
        @RequestParam @Parameter(description = "Årstallet det skal hentes barn for.", example = "2025", required = true) år: Int,
    ): ResponseEntity<Any> = ResponseEntity.ok(aldersjusteringService.hentAlleBarnSomSkalAldersjusteresForÅr(år))

    @GetMapping("/aldersjustering/bidrag/barn/antall")
    @Operation(
        summary = "Hent antall barn som skal aldersjusteres",
        description =
            "Operasjon for å hente antall barn som skal aldersjusteres " +
                "(som fyller 6, 11 eller 15 inneværende år) for et gitt år.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returnerer antall barn som fyller 6, 11 eller 15 for gitt år.",
                content = [Content(schema = Schema(implementation = Int::class))],
            ),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun hentAntallBarnSomSkalAldersjusteres(
        @RequestParam @Parameter(description = "Årstallet det skal hentes antall barn for.", example = "2025", required = true) år: Int,
    ): ResponseEntity<Any> = ResponseEntity.ok(aldersjusteringService.hentAntallBarnSomSkalAldersjusteresForÅr(år))

    @PostMapping("/aldersjustering/bidrag/status")
    @Operation(
        summary = "Hent status for aldersjustering",
        description =
            "Henter status for aldersjustering for barn og sak. Brukes i Bisys for å hente status for aldersjustering.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returnerer aldersjusteringsstatus for hvert barn og sak oppgitt i request.",
                content = [Content(schema = Schema(implementation = AldersjusteringResultatlisteResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun hentAldersjusteringstatusForBarnOgSak(
        @RequestBody request: HentAldersjusteringStatusRequest,
    ): AldersjusteringResultatlisteResponse = aldersjusteringService.hentAldersjusteringstatusForBarnOgSak(request)

    @PostMapping("/aldersjustering/bidrag/{saksnummer}")
    @Operation(
        summary = "Kjør aldersjustering for én bidragssak",
        description =
            "Kjører aldersjustering for én spesifikk bidragssak. " +
                "Aldersjusterer barnebidrag når barn fyller 6, 11 eller 15 år. " +
                "Dersom simuler=true fattes det ikke vedtak, men resultatet returneres.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Aldersjustering gjennomført. Returnerer antall aldersjusterte og ikke-aldersjusterte stønadsposter.",
                content = [Content(schema = Schema(implementation = AldersjusteringResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun aldersjusterBidragSak(
        @PathVariable saksnummer: Saksnummer,
        @RequestParam(required = false) år: Int?,
        @RequestParam(required = false) simuler: Boolean = true,
    ): AldersjusteringResponse =
        aldersjusteringService.kjørAldersjusteringForSak(saksnummer, år ?: YearMonth.now().year, simuler, Stønadstype.BIDRAG)

    @PostMapping("/aldersjustering/bidrag/saker")
    @Operation(
        summary = "Kjør aldersjustering for flere bidragssaker",
        description =
            "Kjører aldersjustering for en liste med bidragssaker. " +
                "Aldersjusterer barnebidrag når barn fyller 6, 11 eller 15 år. " +
                "Dersom simuler=true fattes det ikke vedtak, men resultater returneres per sak.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Aldersjustering gjennomført. Returnerer map fra saksnummer til aldersjusteringsresultat per sak.",
            ),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun aldersjusterBidragSaker(
        @RequestBody saksnummere: List<Saksnummer>,
        @RequestParam(required = false) år: Int?,
        @RequestParam(required = false) simuler: Boolean = true,
    ): Map<Saksnummer, AldersjusteringResponse> =
        saksnummere.associateWith {
            aldersjusteringService.kjørAldersjusteringForSak(
                it,
                år ?: YearMonth.now().year,
                simuler,
                Stønadstype.BIDRAG,
            )
        }
}
