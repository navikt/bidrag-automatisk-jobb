package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
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
@Tag(
    name = "Debug aldersjustering bidrag",
    description = "Debug-endepunkter for aldersjustering av bidrag. Returnerer detaljert informasjon til bruk ved feilsøking.",
)
class AldersjusteringBidragDebugController(
    private val aldersjusteringService: AldersjusteringService,
) {
    @PostMapping("/aldersjustering/bidrag/saker/debug")
    @Operation(
        summary = "Kjør aldersjustering i debug-modus for flere bidragssaker",
        description =
            "Kjører aldersjustering i debug-modus for en liste med bidragssaker. " +
                "Returnerer detaljert informasjon om aldersjusteringsgrunnlag og beregninger til bruk ved feilsøking. " +
                "Dersom simuler=true fattes det ikke vedtak.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Debug-kjøring gjennomført. Returnerer map fra saksnummer til detaljert aldersjusteringsresultat per sak.",
            ),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun aldersjusterBidragSakerDebug(
        @RequestBody saksnummere: List<Saksnummer>,
        @RequestParam(
            required = false,
        ) @Parameter(description = "Årstallet aldersjusteringen gjelder for. Default er inneværende år.", example = "2025") år: Int?,
        @RequestParam(
            required = false,
        ) @Parameter(
            description = "Dersom true simuleres aldersjusteringen uten å fatte vedtak. Default er true.",
            example = "true",
        ) simuler: Boolean = true,
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
        summary = "Kjør aldersjustering i debug-modus for én bidragssak",
        description =
            "Kjører aldersjustering i debug-modus for én spesifikk bidragssak. " +
                "Returnerer detaljert informasjon om aldersjusteringsgrunnlag og beregninger til bruk ved feilsøking. " +
                "Dersom simuler=true fattes det ikke vedtak.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Debug-kjøring gjennomført. Returnerer detaljert aldersjusteringsresultat for saken.",
                content = [Content(schema = Schema(implementation = AldersjusteringResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun aldersjusterBidragSakDebug(
        @PathVariable @Parameter(description = "Saksnummeret for bidragssaken.", example = "2600001", required = true) saksnummer:
            Saksnummer,
        @RequestParam(
            required = false,
        ) @Parameter(description = "Årstallet aldersjusteringen gjelder for. Default er inneværende år.", example = "2025") år: Int?,
        @RequestParam(
            required = false,
        ) @Parameter(
            description = "Dersom true simuleres aldersjusteringen uten å fatte vedtak. Default er true.",
            example = "true",
        ) simuler: Boolean = true,
    ): AldersjusteringResponse =
        aldersjusteringService.kjørAldersjusteringForSakDebug(saksnummer, år ?: YearMonth.now().year, simuler, Stønadstype.BIDRAG)
}
