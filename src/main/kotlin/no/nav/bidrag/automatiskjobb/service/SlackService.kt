package no.nav.bidrag.automatiskjobb.service

import com.slack.api.Slack
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SlackService(
    @param:Value("\${BIDRAG_BOT_SLACK_OAUTH_TOKEN}") private val oauthToken: String,
    @param:Value("\${SLACK_CHANNEL_ID}") private val channel: String,
) {
    fun sendMelding(message: String) {
        Slack.getInstance().methods(oauthToken).chatPostMessage {
            it
                .channel(channel)
                .text(message)
        }
    }
}
