package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.generer.testdata.person.genererPersonident
import no.nav.bidrag.generer.testdata.sak.genererSaksnummer
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class IndeksreguleringBidragServiceTest {
    @MockK
    private lateinit var beløpshistorikkConsumer: BidragBeløpshistorikkConsumer

    @InjectMockKs
    private lateinit var service: IndeksreguleringBidragService

    private val år = 2026
    private val batchId = "test-batch"

    private fun barn() =
        Barn(
            saksnummer = genererSaksnummer(),
            kravhaver = genererPersonident().verdi,
            skyldner = genererPersonident().verdi,
        )

    private fun stønadDto(nesteIndeksreguleringsår: Int?): StønadDto =
        mockk<StønadDto>(relaxed = true) {
            every { this@mockk.nesteIndeksreguleringsår } returns nesteIndeksreguleringsår
        }

    private fun stubHentLøpendeStønad(
        barn: Barn,
        stønadstype: Stønadstype,
        nesteIndeksreguleringsår: Int?,
    ) {
        every {
            beløpshistorikkConsumer.hentLøpendeStønad(
                match<HentStønadRequest> {
                    it.type == stønadstype &&
                        it.kravhaver.verdi == barn.kravhaver &&
                        it.skyldner.verdi == barn.skyldner &&
                        it.sak.verdi == barn.saksnummer
                },
            )
        } returns stønadDto(nesteIndeksreguleringsår)
    }

    @Test
    fun `oppretter indeksregulering når nesteIndeksreguleringsår er lik gjeldende år`() {
        val barn = barn()
        stubHentLøpendeStønad(barn, Stønadstype.BIDRAG, år)

        val resultat = service.opprettIndeksregulering(batchId, år, barn, listOf(Stønadstype.BIDRAG))

        resultat!! shouldHaveSize 1
        resultat.first().let {
            it.barn shouldBe barn
            it.stønadstype shouldBe Stønadstype.BIDRAG
            it.år shouldBe år
            it.batchId shouldBe batchId
            it.status shouldBe Status.UBEHANDLET
        }
    }

    @Test
    fun `oppretter indeksregulering når nesteIndeksreguleringsår er lavere enn gjeldende år`() {
        val barn = barn()
        stubHentLøpendeStønad(barn, Stønadstype.BIDRAG, år - 1)

        val resultat = service.opprettIndeksregulering(batchId, år, barn, listOf(Stønadstype.BIDRAG))

        resultat!! shouldHaveSize 1
    }

    @Test
    fun `hopper over stønadstype når nesteIndeksreguleringsår er null`() {
        val barn = barn()
        every { beløpshistorikkConsumer.hentLøpendeStønad(any()) } returns stønadDto(null)

        val resultat = service.opprettIndeksregulering(batchId, år, barn, listOf(Stønadstype.BIDRAG))

        resultat!!.shouldBeEmpty()
    }

    @Test
    fun `hopper over stønadstype når hentLøpendeStønad returnerer null`() {
        val barn = barn()
        every { beløpshistorikkConsumer.hentLøpendeStønad(any()) } returns null

        val resultat = service.opprettIndeksregulering(batchId, år, barn, listOf(Stønadstype.BIDRAG))

        resultat!!.shouldBeEmpty()
    }

    @Test
    fun `hopper over stønadstype når nesteIndeksreguleringsår er frem i tid`() {
        val barn = barn()
        stubHentLøpendeStønad(barn, Stønadstype.BIDRAG, år + 1)

        val resultat = service.opprettIndeksregulering(batchId, år, barn, listOf(Stønadstype.BIDRAG))

        resultat!!.shouldBeEmpty()
    }

    @Test
    fun `oppretter én indeksregulering per stønadstype`() {
        val barn = barn()
        stubHentLøpendeStønad(barn, Stønadstype.BIDRAG, år)
        stubHentLøpendeStønad(barn, Stønadstype.BIDRAG18AAR, år)

        val resultat = service.opprettIndeksregulering(batchId, år, barn, listOf(Stønadstype.BIDRAG, Stønadstype.BIDRAG18AAR))

        resultat!! shouldHaveSize 2
        resultat.map { it.stønadstype } shouldBe listOf(Stønadstype.BIDRAG, Stønadstype.BIDRAG18AAR)
    }

    @Test
    fun `inkluderer kun stønadstyper som skal indeksreguleres`() {
        val barn = barn()
        stubHentLøpendeStønad(barn, Stønadstype.BIDRAG, år)
        stubHentLøpendeStønad(barn, Stønadstype.BIDRAG18AAR, år + 1)

        val resultat = service.opprettIndeksregulering(batchId, år, barn, listOf(Stønadstype.BIDRAG, Stønadstype.BIDRAG18AAR))

        resultat!! shouldHaveSize 1
        resultat.first().stønadstype shouldBe Stønadstype.BIDRAG
    }

    @Test
    fun `returnerer tom liste når ingen stønadstyper er oppgitt`() {
        val barn = barn()

        val resultat = service.opprettIndeksregulering(batchId, år, barn, emptyList())

        resultat!!.shouldBeEmpty()
    }
}
