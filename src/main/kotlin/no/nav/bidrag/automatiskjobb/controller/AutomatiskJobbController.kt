package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringResponse
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.sak.Saksnummer
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
class AutomatiskJobbController(
    private val aldersjusteringService: AldersjusteringService,
) {
    @GetMapping("/barn")
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

    @GetMapping("/aldersjuster")
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
            ),
            ApiResponse(
                responseCode = "204",
                description = "Fant ingen aldersjustering for id.",
            ),
        ],
    )
    fun hentAldersjustering(id: Int): ResponseEntity<Any> {
        val aldersjustering = aldersjusteringService.hentAldersjustering(id)
        if (aldersjustering == null) {
            return ResponseEntity.noContent().build()
        }
        return ResponseEntity.ok(aldersjustering)
    }

    @PostMapping("/aldersjuster")
    @Operation(
        summary = "Legger til innslag i aldersjusteringstabellen",
        description = "Operasjon for å legge til nye innslag i aldersjusteringstabellen.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Opprettet aldersjustering.",
            ),
        ],
    )
    fun lagreAldersjustering(aldersjustering: Aldersjustering): ResponseEntity<Any> =
        ResponseEntity.ok(aldersjusteringService.lagreAldersjustering(aldersjustering))

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
        @RequestParam(required = false) stønadstype: Stønadstype = Stønadstype.BIDRAG,
    ): AldersjusteringResponse =
        aldersjusteringService.kjørAldersjusteringForSak(saksnummer, år ?: YearMonth.now().year, simuler, Stønadstype.BIDRAG)

    @PostMapping("/aldersjuster/bidrag/saker")
    @Operation(
        summary = "Start kjøring av aldersjustering batch for bidrag.",
        description = "Operasjon for å starte kjøring av aldersjustering batch for bidrag for et gitt år.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun aldersjusterBidragSaker(
        @RequestBody saksnummere: List<Saksnummer>,
        @RequestParam(required = false) år: Int?,
        @RequestParam(required = false) simuler: Boolean = true,
        @RequestParam(required = false) stønadstype: Stønadstype = Stønadstype.BIDRAG,
    ): Map<Saksnummer, AldersjusteringResponse> =
        saksnummere.associateWith {
            aldersjusteringService.kjørAldersjusteringForSak(
                it,
                år ?: YearMonth.now().year,
                simuler,
                Stønadstype.BIDRAG,
            )
        }

    @PostMapping("/aldersjuster/bidrag/{saksnummer}/debug")
    @Operation(
        summary = "Start kjøring av aldersjustering batch for bidrag.",
        description = "Operasjon for å starte kjøring av aldersjustering batch for bidrag for et gitt år.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun aldersjusterBidragSakDebug(
        @PathVariable saksnummer: Saksnummer,
        @RequestParam(required = false) år: Int?,
        @RequestParam(required = false) simuler: Boolean = true,
        @RequestParam(required = false) stønadstype: Stønadstype = Stønadstype.BIDRAG,
    ): AldersjusteringResponse =
        aldersjusteringService.kjørAldersjusteringForSakDebug(saksnummer, år ?: YearMonth.now().year, simuler, Stønadstype.BIDRAG)

    @PostMapping("/aldersjuster/bidrag/saker/debug")
    @Operation(
        summary = "Start kjøring av aldersjustering batch for bidrag.",
        description = "Operasjon for å starte kjøring av aldersjustering batch for bidrag for et gitt år.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun aldersjusterBidragSakerDebug(
        @RequestBody saksnummere: List<Saksnummer>,
        @RequestParam(required = false) år: Int?,
        @RequestParam(required = false) simuler: Boolean = true,
        @RequestParam(required = false) stønadstype: Stønadstype = Stønadstype.BIDRAG,
    ): Map<Saksnummer, AldersjusteringResponse> =
        saksnummere.associateWith {
            aldersjusteringService.kjørAldersjusteringForSakDebug(
                it,
                år ?: YearMonth.now().year,
                simuler,
                Stønadstype.BIDRAG,
            )
        }

    @GetMapping("/barn/antall")
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
                description = "Returnerer antall barn som fyller 6, 11 eller 15 for gitt år år.",
            ),
        ],
    )
    fun hentAntallBarnSomSkalAldersjusteres(år: Int): ResponseEntity<Any> =
        ResponseEntity.ok(aldersjusteringService.hentAntallBarnSomSkalAldersjusteresForÅr(år))
}
