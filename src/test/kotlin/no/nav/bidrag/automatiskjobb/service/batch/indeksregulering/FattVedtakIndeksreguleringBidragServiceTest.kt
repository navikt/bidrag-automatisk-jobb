package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class FattVedtakIndeksreguleringBidragServiceTest {
    @MockK
    private lateinit var bidragVedtakConsumer: BidragVedtakConsumer

    @MockK
    private lateinit var indeksreguleringRepository: IndeksreguleringRepository

    @InjectMockKs
    private lateinit var service: FattVedtakIndeksreguleringBidragService

    private fun indeksregulering(vedtak: Int? = 999) =
        Indeksregulering(
            batchId = "batch",
            år = 2026,
            barn = Barn(saksnummer = "2600001", kravhaver = "22222222222", skyldner = "11111111111"),
            stønadstype = Stønadstype.BIDRAG,
            status = Status.BEHANDLET,
            behandlingstype = Behandlingstype.FATTET_FORSLAG,
            vedtak = vedtak,
        )

    @Test
    fun `skal fatte vedtaksforslag og oppdatere status til FATTET`() {
        val indeksregulering = indeksregulering()
        every { bidragVedtakConsumer.fatteVedtaksforslag(999) } returns 999
        every { indeksreguleringRepository.save(any()) } answers { firstArg() }

        service.fattVedtak(indeksregulering, simuler = false)

        indeksregulering.status shouldBe Status.FATTET
        indeksregulering.gjennomfort shouldBe true
        indeksregulering.fattetTidspunkt.shouldNotBeNull()
        verify(exactly = 1) { indeksreguleringRepository.save(indeksregulering) }
    }

    @Test
    fun `skal ikke fatte vedtaksforslag ved simulering`() {
        val indeksregulering = indeksregulering()

        service.fattVedtak(indeksregulering, simuler = true)

        indeksregulering.status shouldBe Status.BEHANDLET
        indeksregulering.gjennomfort shouldBe false
        verify(exactly = 0) { bidragVedtakConsumer.fatteVedtaksforslag(any()) }
        verify(exactly = 0) { indeksreguleringRepository.save(any()) }
    }

    @Test
    fun `skal sette FATTE_VEDTAK_FEILET og kaste feilen videre når fatting feiler`() {
        val indeksregulering = indeksregulering()
        every { bidragVedtakConsumer.fatteVedtaksforslag(999) } throws RuntimeException("Fatting feilet")
        every { indeksreguleringRepository.save(any()) } answers { firstArg() }

        shouldThrow<RuntimeException> { service.fattVedtak(indeksregulering, simuler = false) }

        indeksregulering.status shouldBe Status.FATTE_VEDTAK_FEILET
        indeksregulering.gjennomfort shouldBe false
        verify(exactly = 1) { indeksreguleringRepository.save(indeksregulering) }
    }

    @Test
    fun `skal sette FATTE_VEDTAK_FEILET når vedtak mangler`() {
        val indeksregulering = indeksregulering(vedtak = null)
        every { indeksreguleringRepository.save(any()) } answers { firstArg() }

        shouldThrow<IllegalStateException> { service.fattVedtak(indeksregulering, simuler = false) }

        indeksregulering.status shouldBe Status.FATTE_VEDTAK_FEILET
        verify(exactly = 0) { bidragVedtakConsumer.fatteVedtaksforslag(any()) }
    }
}
