package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
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

data class AldersjusteringerHvorBeløpBleRedusertRespons(
    val antall: Int,
    val saker: List<Map<String, Any?>>,
)

@Protected
@RestController
@Tag(name = "Debug aldersjustering bidrag")
class AldersjusteringBidragDebugController(
    private val aldersjusteringService: AldersjusteringService,
) {
    @PostMapping("/aldersjustering/bidrag/saker/debug")
    @Operation(
        summary = "Start kjøring av aldersjustering batch for bidrag.",
        description = "Operasjon for å starte kjøring av aldersjustering batch for bidrag for et gitt år.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun aldersjusterBidragSakerDebug(
        @RequestBody saksnummere: List<Saksnummer>,
        @RequestParam(required = false) år: Int?,
        @RequestParam(required = false) simuler: Boolean = true,
    ): Map<Saksnummer, AldersjusteringResponse> =
        saksnummere.associateWith {
            aldersjusteringService.kjørAldersjusteringForSakDebug(
                it,
                år ?: YearMonth.now().year,
                simuler,
                Stønadstype.BIDRAG,
            )
        }

    @GetMapping("/aldersjustering/bidrag/redusert")
    @Operation(
        summary = "Hent aldersjusteringer hvor beløp er redusert",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    suspend fun hentAlleAldersjusteringerHvorBeløpErRedusert() = aldersjusteringService.hentAlleAldersjusteringerHvorBeløpErRedusert()

    @PostMapping("/aldersjustering/bidrag/{saksnummer}/debug")
    @Operation(
        summary = "Start kjøring av aldersjustering batch for bidrag.",
        description = "Operasjon for å starte kjøring av aldersjustering batch for bidrag for et gitt år.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun aldersjusterBidragSakDebug(
        @PathVariable saksnummer: Saksnummer,
        @RequestParam(required = false) år: Int?,
        @RequestParam(required = false) simuler: Boolean = true,
    ): AldersjusteringResponse =
        aldersjusteringService.kjørAldersjusteringForSakDebug(saksnummer, år ?: YearMonth.now().year, simuler, Stønadstype.BIDRAG)

    @PostMapping("/aldersjustering/bidrag/verifiser")
    @Operation(
        summary = "Verifiser aldersjusteringer for et år",
        description =
            "Starter verifisering i bakgrunnen. Kjører beregning på nytt for alle FATTET-vedtak for et gitt år og " +
                "logger avvik i KOPI_SAMVÆRSPERIODE eller KOPI_DELBEREGNING_UNDERHOLDSKOSTNAD. " +
                "Returnerer 202 Accepted umiddelbart — se applikasjonsloggen for resultat.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun startVerifiserAldersjusteringerForÅr(
        @RequestParam år: Int,
    ): ResponseEntity<Void> {
        aldersjusteringService.startVerifiserAldersjusteringerForÅr(år)
        return ResponseEntity.accepted().build()
    }
}
