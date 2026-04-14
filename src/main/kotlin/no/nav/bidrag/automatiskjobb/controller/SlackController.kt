package no.nav.bidrag.automatiskjobb.controller

import no.nav.bidrag.automatiskjobb.service.SlackService
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
class SlackController(
    private val slackService: SlackService,
) {
    @PostMapping("/slack/melding")
    fun sendMelding(
        @RequestBody melding: String,
    ) {
        slackService.sendMelding(melding)
    }
}
