package no.nav.bidrag.automatiskjobb.controller

import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchKjøreplanVarsler
import no.nav.bidrag.automatiskjobb.service.SlackService
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
class SlackController(
    private val slackService: SlackService,
    private val batchKjøreplanVarsler: BatchKjøreplanVarsler
) {
    @PostMapping("/slack/melding")
    fun sendMelding(melding: String) {
        slackService.sendMelding(melding)
    }

    @PostMapping("/slack/kjøreplan")
    fun sendKjøreplan() {
        batchKjøreplanVarsler.sendBatchKjøreplanVarsel()
    }
}
