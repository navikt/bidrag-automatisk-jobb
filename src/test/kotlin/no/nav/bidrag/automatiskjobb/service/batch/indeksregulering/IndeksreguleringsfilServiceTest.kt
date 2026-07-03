package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.filoverforing.FiloverføringTilElinKlient
import no.nav.bidrag.automatiskjobb.persistence.bucket.ByteArrayOutputStreamTilByteBuffer
import no.nav.bidrag.automatiskjobb.persistence.bucket.GcpFilBucket
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import no.nav.bidrag.domene.enums.adresse.Adressetype
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.land.Landkode2
import no.nav.bidrag.domene.land.Landkode3
import no.nav.bidrag.transport.person.PersonAdresseDto
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersondetaljerDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class IndeksreguleringsfilServiceTest {
    @MockK
    private lateinit var indeksreguleringRepository: IndeksreguleringRepository

    @MockK(relaxed = true)
    private lateinit var gcpFilBucket: GcpFilBucket

    @MockK(relaxed = true)
    private lateinit var bidragPersonConsumer: BidragPersonConsumer

    @MockK(relaxed = true)
    private lateinit var filoverføringTilElinKlient: FiloverføringTilElinKlient

    @InjectMockKs
    private lateinit var service: IndeksreguleringsfilService

    private fun barn(
        saksnummer: String,
        kravhaver: String,
        skyldner: String?,
    ) = Barn(saksnummer = saksnummer, kravhaver = kravhaver, skyldner = skyldner)

    private fun persondetaljer(
        fnr: String,
        landAlpha2: String = "NO",
        landAlpha3: String = "NOR",
        diskresjonskode: Diskresjonskode? = null,
        harAdresse: Boolean = true,
    ) = PersondetaljerDto(
        person = PersonDto(ident = Personident(fnr), diskresjonskode = diskresjonskode),
        adresse =
            if (harAdresse) {
                PersonAdresseDto(
                    adressetype = Adressetype.BOSTEDSADRESSE,
                    adresselinje1 = "Gate 1",
                    land = Landkode2(landAlpha2),
                    land3 = Landkode3(landAlpha3),
                )
            } else {
                null
            },
        kontonummer = null,
        dødsbo = null,
        språk = null,
        tidligereIdenter = null,
    )

    private fun indeksregulering(barn: Barn) =
        Indeksregulering(
            batchId = "batch",
            år = 2026,
            barn = barn,
            stønadstype = Stønadstype.BIDRAG,
            status = Status.FATTET,
            behandlingstype = Behandlingstype.FATTET_FORSLAG,
            gjennomfort = true,
            beløp = BigDecimal.valueOf(2000),
        )

    @Test
    fun `bygger en linje per indeksregulering og legger norske linjer i reskontro og elin`() {
        every {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndStønadstypeInAndÅr(
                Status.FATTET,
                Behandlingstype.FATTET_FORSLAG,
                listOf(Stønadstype.BIDRAG, Stønadstype.OPPFOSTRINGSBIDRAG, Stønadstype.BIDRAG18AAR),
                2026,
            )
        } returns
            listOf(
                indeksregulering(barn("2600001", "22222222222", "11111111111")),
                indeksregulering(barn("2600001", "33333333333", "11111111111")),
            )
        every { bidragPersonConsumer.hentPersondetaljer(Personident("11111111111")) } returns persondetaljer("11111111111")

        val data = service.byggRapportData(2026)

        data.bidragsreskontro shouldHaveSize 2
        data.elin shouldHaveSize 2
        data.bpUtlandBrev shouldHaveSize 0
        data.bidragsreskontro.first().fnrBp shouldBe "11111111111"
        data.bidragsreskontro.first().fnrBa shouldBe "22222222222"
        data.bidragsreskontro.first().landkode shouldBe "NO"
    }

    @Test
    fun `hopper over barn uten skyldner`() {
        every {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndStønadstypeInAndÅr(
                Status.FATTET,
                Behandlingstype.FATTET_FORSLAG,
                listOf(Stønadstype.BIDRAG, Stønadstype.OPPFOSTRINGSBIDRAG, Stønadstype.BIDRAG18AAR),
                2026,
            )
        } returns listOf(indeksregulering(barn("2600001", "22222222222", null)))

        service.byggRapportData(2026).elin shouldHaveSize 0
    }

    @Test
    fun `hopper over barn med blank skyldner`() {
        every {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndStønadstypeInAndÅr(
                Status.FATTET,
                Behandlingstype.FATTET_FORSLAG,
                listOf(Stønadstype.BIDRAG, Stønadstype.OPPFOSTRINGSBIDRAG, Stønadstype.BIDRAG18AAR),
                2026,
            )
        } returns listOf(indeksregulering(barn("2600001", "22222222222", "   ")))

        val data = service.byggRapportData(2026)

        data.elin shouldHaveSize 0
        data.bidragsreskontro shouldHaveSize 0
    }

    @Test
    fun `returnerer tomme rapportlister når det ikke finnes gjennomførte indeksreguleringer`() {
        every {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndStønadstypeInAndÅr(
                Status.FATTET,
                Behandlingstype.FATTET_FORSLAG,
                listOf(Stønadstype.BIDRAG, Stønadstype.OPPFOSTRINGSBIDRAG, Stønadstype.BIDRAG18AAR),
                2026,
            )
        } returns emptyList()

        val data = service.byggRapportData(2026)

        data.bidragsreskontro shouldHaveSize 0
        data.elin shouldHaveSize 0
        data.bpUtlandBrev shouldHaveSize 0
        data.bpUtlandDiskresjon shouldHaveSize 0
        data.bpUtlandManglerAdresse shouldHaveSize 0
    }

    @Test
    fun `filtrerer bort kun radene uten skyldner når resultatet inneholder en blanding`() {
        every {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndStønadstypeInAndÅr(
                Status.FATTET,
                Behandlingstype.FATTET_FORSLAG,
                listOf(Stønadstype.BIDRAG, Stønadstype.OPPFOSTRINGSBIDRAG, Stønadstype.BIDRAG18AAR),
                2026,
            )
        } returns
            listOf(
                indeksregulering(barn("2600001", "22222222222", "11111111111")),
                indeksregulering(barn("2600002", "44444444444", null)),
                indeksregulering(barn("2600003", "55555555555", "   ")),
            )
        every { bidragPersonConsumer.hentPersondetaljer(Personident("11111111111")) } returns persondetaljer("11111111111")

        val data = service.byggRapportData(2026)

        data.elin shouldHaveSize 1
        data.bidragsreskontro shouldHaveSize 1
        data.bidragsreskontro.first().fnrBa shouldBe "22222222222"
    }

    @Test
    fun `kaster ikke feil men returnerer tom liste for et annet år uten data`() {
        every {
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndStønadstypeInAndÅr(
                Status.FATTET,
                Behandlingstype.FATTET_FORSLAG,
                listOf(Stønadstype.BIDRAG, Stønadstype.OPPFOSTRINGSBIDRAG, Stønadstype.BIDRAG18AAR),
                2027,
            )
        } returns emptyList()

        val data = service.byggRapportData(2027)

        data.elin shouldHaveSize 0
        data.bidragsreskontro shouldHaveSize 0
    }

    @Test
    fun `streamer fil med riktig navn og innhold til bucket`() {
        val filnavnSlot = slot<String>()
        val bufferSlot = slot<ByteArrayOutputStreamTilByteBuffer>()

        val skrevet = service.lastOppFil("indeksregulering-bidrag/", "bidragsreskontro", LocalDate.of(2026, 7, 1), "innhold")

        skrevet shouldBe true
        verify { gcpFilBucket.lagreFil(capture(filnavnSlot), capture(bufferSlot), contentType = "text/plain") }
        filnavnSlot.captured shouldBe "indeksregulering-bidrag/bidragsreskontro-20260701.txt"
        bufferSlot.captured.toByteArray().toString(Charsets.UTF_8) shouldBe "innhold"
    }
}
