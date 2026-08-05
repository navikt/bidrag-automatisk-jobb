package no.nav.bidrag.automatiskjobb.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchKjøreplanVarsler
import no.nav.bidrag.commons.service.slack.SlackService
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Tag(name = "Slack", description = "Endepunkter for å sende meldinger til Slack")
class SlackController(
    private val slackService: SlackService,
    private val batchKjøreplanVarsler: BatchKjøreplanVarsler,
) {
    @PostMapping("/slack/melding")
    @Operation(
        summary = "Send melding til Slack",
        description = "Sender en fritekstmelding til den konfigurerte Slack-kanalen.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Melding sendt til Slack."),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun sendMelding(
        @RequestParam @Parameter(description = "Meldingsteksten som skal sendes til Slack.", required = true) melding: String,
    ) {
        slackService.sendMelding(melding)
    }

    @PostMapping("/slack/kjøreplan")
    @Operation(
        summary = "Send kjøreplan-varsel til Slack",
        description = "Sender en oversikt over planlagte batch-kjøringer til Slack-kanalen.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Kjøreplan-varsel sendt til Slack."),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun sendKjøreplan() {
        batchKjøreplanVarsler.sendBatchKjøreplanVarsel()
    }
}
