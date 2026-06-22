package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.automatiskjobb.batch.utils.forsendelse.distribuer.DistribuerForsendelseBidragBatch
import no.nav.bidrag.automatiskjobb.batch.utils.forsendelse.opprett.OpprettForsendelseBatch
import no.nav.bidrag.automatiskjobb.batch.utils.forsendelse.slett.SlettForsendelseSomSkalSlettesBatch
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Tag(name = "Forsendelse batch")
class ForsendelseBatchController(
    private val slettForsendelseSomSkalSlettesBatch: SlettForsendelseSomSkalSlettesBatch,
    private val opprettForsendelseBatch: OpprettForsendelseBatch,
    private val distribuerForsendelseBidragBatch: DistribuerForsendelseBidragBatch,
) {
    @PostMapping("/batch/forsendelse/slett")
    @Operation(
        summary = "Start kjøring av batch som sletter forsendelser med skal_slettes=true eller spesifikke forsendelse bestillinger.",
        description =
            "Sletter forsendelser basert på kriterier. " +
                "Uten parametere slettes alle forsendelser som er markert med skal_slettes=true. " +
                "Med bestillingIder slettes kun forsendelsene for de angitte IDene fra forsendelse_bestilling tabellen.",
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
    fun starSlettForsendelserSomSkalSlettesBatch(
        @RequestParam
        @Parameter(
            description =
                "Komma-separert liste over ID fra forsendelse_bestilling tabellen som skal slettes. " +
                    "Eksempel: 1,2,3. Hvis ikke angitt slettes alle forsendelser med skal_slettes=true",
        )
        bestillingIder: String? = null,
    ): ResponseEntity<Any> {
        slettForsendelseSomSkalSlettesBatch.start(bestillingIder)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/batch/forsendelse/opprett")
    @Operation(
        summary = "Start kjøring av batch for å opprette forsendelser.",
        description =
            "Oppretter forsendelser for vedtak. " +
                "Uten bestillingIder opprettes forsendelser for alle vedtak som ikke har forsendelse opprettet. " +
                "Med bestillingIder slettes og gjennopprettes forsendelsene for de IDene fra forsendelse_bestilling tabellen.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for oppretting av forsendelser ble startet.",
            ),
        ],
    )
    fun startOpprettForsendelseAldersjusteringBidragBatch(
        @RequestParam
        @Parameter(description = "Prosessér forsendelser som tidligere har feilet")
        prosesserFeilet: Boolean = false,
        @RequestParam
        @Parameter(
            description =
                "Komma-separert liste over ID fra forsendelse_bestilling tabellen som skal slettes og gjennopprettes. " +
                    "Eksempel: 1,2,3. Hvis ikke angitt opprettes det for alle forsendelsebestillinger uten forsendelse_id",
        )
        bestillingIder: String? = null,
    ): ResponseEntity<Any> {
        opprettForsendelseBatch.start(prosesserFeilet, bestillingIder)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/batch/forsendelse/distribuer")
    @Operation(
        summary = "Start kjøring av batch for å distribuere forsendelser.",
        description =
            "Distribuerer forsendelser til mottakerne (bidragspliktig og/eller bidragsmottaker). " +
                "Uten bestillingIder distribueres alle forsendelser som er opprettet men ikke distribuert. " +
                "Med bestillingIder distribueres kun forsendelsene for de angitte IDene fra forsendelse_bestilling tabellen.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Batch for distribusjon av forsendelser ble startet.",
            ),
        ],
    )
    fun startDistribuerForsendelseAldersjusteringBidragBatch(
        @RequestParam
        @Parameter(
            description =
                "Komma-separert liste over ID fra forsendelse_bestilling tabellen som skal distribueres. " +
                    "Eksempel: 1,2,3. Hvis ikke angitt distribueres alle opprettede forsendelser som ikke er distribuert",
        )
        bestillingIder: String? = null,
    ): ResponseEntity<Any> {
        distribuerForsendelseBidragBatch.start(bestillingIder)
        return ResponseEntity.ok().build()
    }
}
