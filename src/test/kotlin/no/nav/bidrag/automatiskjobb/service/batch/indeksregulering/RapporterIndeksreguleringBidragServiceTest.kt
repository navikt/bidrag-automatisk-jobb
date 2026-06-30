package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class RapporterIndeksreguleringBidragServiceTest {
    @MockK
    private lateinit var indeksreguleringRepository: IndeksreguleringRepository

    @InjectMockKs
    private lateinit var service: RapporterIndeksreguleringBidragService

    private fun barn(
        saksnummer: String,
        kravhaver: String,
        skyldner: String?,
    ) = Barn(saksnummer = saksnummer, kravhaver = kravhaver, skyldner = skyldner)

    private fun indeksregulering(barn: List<Barn>) =
        Indeksregulering(
            batchId = "batch",
            saksnummer = barn.first().saksnummer,
            år = 2026,
            barn = barn.toMutableList(),
            stønadstype = Stønadstype.BIDRAG,
            status = Status.UBEHANDLET,
            gjennomfort = true,
        )

    @Test
    fun `bygger en linje per barn og legger norske linjer i reskontro og elin`() {
        every {
            indeksreguleringRepository.findAllByGjennomfortTrueAndStønadstypeAndÅr(Stønadstype.BIDRAG, 2026)
        } returns
            listOf(
                indeksregulering(
                    listOf(
                        barn("2600001", "22222222222", "11111111111"),
                        barn("2600001", "33333333333", "11111111111"),
                    ),
                ),
            )

        val data = service.byggRapportData(2026)

        data.bidragsreskontro shouldHaveSize 2
        data.elin shouldHaveSize 2
        data.bpUtlandBrev shouldHaveSize 0
        data.bidragsreskontro.first().fnrBp shouldBe "11111111111"
        data.bidragsreskontro.first().fnrBa shouldBe "22222222222"
        data.bidragsreskontro.first().landkode shouldBe LANDKODE_NORGE
    }

    @Test
    fun `hopper over barn uten skyldner`() {
        every {
            indeksreguleringRepository.findAllByGjennomfortTrueAndStønadstypeAndÅr(Stønadstype.BIDRAG, 2026)
        } returns listOf(indeksregulering(listOf(barn("2600001", "22222222222", null))))

        service.byggRapportData(2026).elin shouldHaveSize 0
    }
}
