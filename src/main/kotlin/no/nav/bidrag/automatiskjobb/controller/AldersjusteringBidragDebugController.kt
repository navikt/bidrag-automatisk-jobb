package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.automatiskjobb.service.AldersjusteringService
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringResponse
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

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
}
