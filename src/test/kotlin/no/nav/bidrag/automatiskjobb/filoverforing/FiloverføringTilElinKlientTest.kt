package no.nav.bidrag.automatiskjobb.filoverforing

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.persistence.bucket.GcpFilBucket
import no.nav.bidrag.commons.service.slack.SlackService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Base64

@ExtendWith(MockKExtension::class)
class FiloverføringTilElinKlientTest {
    @MockK(relaxed = true)
    private lateinit var gcpFilBucket: GcpFilBucket

    @MockK(relaxed = true)
    private lateinit var slackService: SlackService

    private fun konfig(skalOverføreFil: Boolean) =
        FiloverføringTilElinConfig(
            username = "srvBidragRegnskap",
            host = "sftp-q.nav.no",
            port = 22,
            privateKey = Base64.getEncoder().encodeToString("ugyldig-test-nøkkel".toByteArray()),
            skalOverforeFil = skalOverføreFil,
        )

    @Test
    fun `laster ikke opp til filsluse når skalOverføreFil er false`() {
        val klient = FiloverføringTilElinKlient(konfig(skalOverføreFil = false), gcpFilBucket, slackService)

        klient.lastOppFilTilFilsluse("mappe/", "fil.txt")

        verify(exactly = 0) { gcpFilBucket.hentFil(any()) }
        verify(exactly = 0) { slackService.sendMelding(any()) }
    }

    @Test
    fun `sender slack-varsel og kaster videre når overforing feiler`() {
        val klient = FiloverføringTilElinKlient(konfig(skalOverføreFil = true), gcpFilBucket, slackService)

        // Ugyldig privatnøkkel/host fører til at JSch feiler før noen SFTP-kobling opprettes.
        assertThrows<Exception> {
            klient.lastOppFilTilFilsluse("mappe/", "fil.txt")
        }

        verify(exactly = 1) { slackService.sendMelding(any()) }
    }
}
