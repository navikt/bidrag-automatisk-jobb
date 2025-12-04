package no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.ForsendelseBestilling
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Forsendelsestype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import no.nav.bidrag.automatiskjobb.service.ForsendelseBestillingService
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.generer.testdata.sak.genererSaksnummer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth
import java.util.UUID

@ExtendWith(MockKExtension::class)
class FattVedtakRevurderForskuddServiceTest {
    @MockK
    private lateinit var bidragVedtakConsumer: BidragVedtakConsumer

    @MockK
    private lateinit var revurderForskuddRepository: RevurderForskuddRepository

    @MockK
    private lateinit var forsendelseBestillingService: ForsendelseBestillingService

    @InjectMockKs
    private lateinit var fattVedtakRevurderForskuddService: FattVedtakRevurderForskuddService

    @Test
    fun `skal ved simulering ikke fatte vedtak`() {
        val revurderingForskudd = opprettRevurderingForskudd()

        fattVedtakRevurderForskuddService.fattVedtak(revurderingForskudd, true)

        verify(exactly = 0) { revurderForskuddRepository.save(any()) }
        revurderingForskudd.status shouldBe Status.BEHANDLET
    }

    @Test
    fun `skal feile om vedtaksid ikke er satt på revurderingForskudd`() {
        val revurderingForskudd = opprettRevurderingForskudd()

        every { revurderForskuddRepository.save(any()) } returns mockk()

        shouldThrow<IllegalStateException> { fattVedtakRevurderForskuddService.fattVedtak(revurderingForskudd, false) }

        verify { revurderForskuddRepository.save(any()) }
        revurderingForskudd.status shouldBe Status.FATTE_VEDTAK_FEILET
    }

    @Test
    fun `skal fatte vedtak om revurdering av forskudd`() {
        val revurderingForskudd = opprettRevurderingForskudd(vedtakId = 123)

        every { revurderForskuddRepository.save(any()) } returns mockk()
        every { bidragVedtakConsumer.fatteVedtaksforslag(123) } returns 1
        every {
            forsendelseBestillingService.opprettBestilling(
                revurderingForskudd,
                Forsendelsestype.REVURDERING_FORSKUDD,
            )
        } returns
            mutableListOf(mockk<ForsendelseBestilling>())

        fattVedtakRevurderForskuddService.fattVedtak(revurderingForskudd, false)

        verify { revurderForskuddRepository.save(any()) }
        revurderingForskudd.status shouldBe Status.FATTET
        revurderingForskudd.forsendelseBestilling shouldHaveSize 1
    }

    private fun opprettRevurderingForskudd(vedtakId: Int? = null): RevurderingForskudd =
        RevurderingForskudd(
            id = 1,
            forMåned = YearMonth.now().toString(),
            batchId = UUID.randomUUID().toString(),
            barn =
                mockk<Barn>(relaxed = true).apply {
                    every { this@apply.kravhaver } returns genererFødselsnummer()
                    every { this@apply.saksnummer } returns genererSaksnummer()
                },
            status = Status.BEHANDLET,
            vedtak = vedtakId,
        )
}
