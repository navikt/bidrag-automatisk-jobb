package no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.consumer.BidragBehandlingConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.beregn.barnebidrag.service.SisteManuelleVedtak
import no.nav.bidrag.beregn.barnebidrag.service.VedtakService
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.transport.behandling.behandling.HentÅpneBehandlingerRespons
import no.nav.bidrag.transport.behandling.behandling.ÅpenBehandling
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class OpprettRevurderForskuddServiceTest {
    @MockK
    private lateinit var bidragBehandlingConsumer: BidragBehandlingConsumer

    @MockK
    private lateinit var vedtakService: VedtakService

    @InjectMockKs
    private lateinit var opprettRevurderForskuddService: OpprettRevurderForskuddService

    @Test
    fun `skal ikke opprette revurdering forskudd om det finnes åpen forskuddssak`() {
        val kravhaver = genererFødselsnummer()
        every { bidragBehandlingConsumer.hentÅpneBehandlingerForBarn(kravhaver) } returns
            mockk<HentÅpneBehandlingerRespons> {
                every { behandlinger } returns
                    listOf(
                        mockk<ÅpenBehandling> {
                            every { stønadstype } returns Stønadstype.FORSKUDD
                        },
                    )
            }

        val barn =
            mockk<Barn>().apply {
                every { this@apply.kravhaver } returns kravhaver
            }

        val resultat = opprettRevurderForskuddService.opprettRevurdereForskudd(barn, "batchId", LocalDateTime.now())

        resultat shouldBe null
    }

    @Test
    fun `skal ikke opprette revudering forskudd om siste manuelle vedtak er etter cutoff tidspunkt`() {
        val kravhaver = genererFødselsnummer()
        val skyldner = genererFødselsnummer()
        val saksnummer = "2500001"
        every { bidragBehandlingConsumer.hentÅpneBehandlingerForBarn(kravhaver) } returns
            mockk<HentÅpneBehandlingerRespons> {
                every { behandlinger } returns emptyList()
            }
        val cutoffTidspunkt = LocalDateTime.now().minusDays(1)
        every { vedtakService.finnSisteManuelleVedtak(any()) } returns
            mockk<SisteManuelleVedtak> {
                every { vedtak.opprettetTidspunkt } returns cutoffTidspunkt.plusHours(1)
            }

        val barn =
            mockk<Barn>().apply {
                every { this@apply.kravhaver } returns kravhaver
                every { this@apply.skyldner } returns skyldner
                every { this@apply.saksnummer } returns saksnummer
            }

        val resultat = opprettRevurderForskuddService.opprettRevurdereForskudd(barn, "batchId", cutoffTidspunkt)

        resultat shouldBe null
        verify(exactly = 1) { vedtakService.finnSisteManuelleVedtak(any()) }
    }

    @Test
    fun `skal opprette revudering forskudd`() {
        val kravhaver = genererFødselsnummer()
        val skyldner = genererFødselsnummer()
        val saksnummer = "2500001"
        every { bidragBehandlingConsumer.hentÅpneBehandlingerForBarn(kravhaver) } returns
            mockk<HentÅpneBehandlingerRespons> {
                every { behandlinger } returns emptyList()
            }
        val cutoffTidspunkt = LocalDateTime.now().minusDays(1)
        every { vedtakService.finnSisteManuelleVedtak(any()) } returns
            mockk<SisteManuelleVedtak> {
                every { vedtak.opprettetTidspunkt } returns cutoffTidspunkt.minusHours(1)
            }

        val barn =
            mockk<Barn>().apply {
                every { this@apply.kravhaver } returns kravhaver
                every { this@apply.skyldner } returns skyldner
                every { this@apply.saksnummer } returns saksnummer
            }

        val resultat = opprettRevurderForskuddService.opprettRevurdereForskudd(barn, "batchId", cutoffTidspunkt)

        resultat shouldNotBe null
        verify(exactly = 1) { vedtakService.finnSisteManuelleVedtak(any()) }
        resultat?.status shouldBe Status.UBEHANDLET
    }
}
