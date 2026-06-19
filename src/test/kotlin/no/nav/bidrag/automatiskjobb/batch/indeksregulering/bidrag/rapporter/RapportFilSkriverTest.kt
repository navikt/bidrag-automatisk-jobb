package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.rapporter

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.persistence.bucket.ByteArrayOutputStreamTilByteBuffer
import no.nav.bidrag.automatiskjobb.persistence.bucket.GcpFilBucket
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class RapportFilSkriverTest {
    @MockK(relaxed = true)
    private lateinit var gcpFilBucket: GcpFilBucket

    private val dato = LocalDate.of(2026, 7, 1)

    private fun filSkriver() = RapportFilSkriver(gcpFilBucket)

    @Test
    fun `streamer fil med riktig navn og innhold til bucket`() {
        val filnavnSlot = slot<String>()
        val bufferSlot = slot<ByteArrayOutputStreamTilByteBuffer>()

        val skrevet = filSkriver().skrivFil("indeksregulering-bidrag/", "bidragsreskontro", dato, "innhold")

        skrevet shouldBe true
        verify { gcpFilBucket.lagreFil(capture(filnavnSlot), capture(bufferSlot), contentType = "text/plain") }
        filnavnSlot.captured shouldBe "indeksregulering-bidrag/bidragsreskontro-20260701.txt"
        bufferSlot.captured.toByteArray().toString(Charsets.UTF_8) shouldBe "innhold"
    }

    @Test
    fun `hopper over skriving naar filnavn mangler`() {
        filSkriver().skrivFil("indeksregulering-bidrag/", null, dato, "x") shouldBe false
        filSkriver().skrivFil("indeksregulering-bidrag/", "", dato, "x") shouldBe false
        verify(exactly = 0) { gcpFilBucket.lagreFil(any(), any(), any()) }
    }

    @Test
    fun `hopper over skriving naar mappe mangler`() {
        filSkriver().skrivFil(null, "elin", dato, "x") shouldBe false
        verify(exactly = 0) { gcpFilBucket.lagreFil(any(), any(), any()) }
    }
}
