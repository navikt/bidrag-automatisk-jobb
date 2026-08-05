package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import com.fasterxml.jackson.databind.node.POJONode
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.mapper.VedtakMapper
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.beregn.barnebidrag.service.external.VedtakService
import no.nav.bidrag.domene.beløp.Beløp
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.indeksregulering.BeregnIndeksreguleringApi
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadPeriodeDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningIndeksregulering
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.bidrag.transport.person.PersonDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class GjennomførOpprettIndeksreguleringBidragServiceTest {
    @MockK
    private lateinit var beregnIndeksreguleringApi: BeregnIndeksreguleringApi

    @MockK
    private lateinit var beløpshistorikkConsumer: BidragBeløpshistorikkConsumer

    @MockK
    private lateinit var personConsumer: BidragPersonConsumer

    @MockK(relaxed = true)
    private lateinit var sakConsumer: BidragSakConsumer

    @MockK
    private lateinit var bidragVedtakConsumer: BidragVedtakConsumer

    @MockK
    private lateinit var vedtakService: VedtakService

    @MockK(relaxed = true)
    private lateinit var vedtakMapper: VedtakMapper

    @InjectMockKs
    private lateinit var service: GjennomførIndeksreguleringBidragService

    private val kravhaver = "22222222222"
    private val skyldner = "11111111111"
    private val mottaker = "33333333333"
    private val saksnummer = "2600001"

    private fun barn(skyldner: String? = this.skyldner) =
        Barn(
            saksnummer = saksnummer,
            kravhaver = kravhaver,
            skyldner = skyldner,
            fødselsdato = LocalDate.of(2015, 3, 1),
        )

    private fun indeksregulering(barn: Barn = barn()) =
        Indeksregulering(
            batchId = "batch",
            år = 2026,
            barn = barn,
            stønadstype = Stønadstype.BIDRAG,
            status = Status.UBEHANDLET,
        )

    private fun stønad(valutakode: String? = "NOK") =
        StønadDto(
            stønadsid = 1,
            type = Stønadstype.BIDRAG,
            sak = Saksnummer(saksnummer),
            skyldner = Personident(skyldner),
            kravhaver = Personident(kravhaver),
            mottaker = Personident(mottaker),
            førsteIndeksreguleringsår = 2026,
            nesteIndeksreguleringsår = 2026,
            innkreving = Innkrevingstype.MED_INNKREVING,
            opprettetAv = "test",
            opprettetTidspunkt = LocalDateTime.now(),
            endretAv = null,
            endretTidspunkt = null,
            periodeListe =
                listOf(
                    StønadPeriodeDto(
                        periodeid = 1,
                        periode = ÅrMånedsperiode(YearMonth.of(2025, 7), null),
                        stønadsid = 1,
                        vedtaksid = 10,
                        gyldigFra = LocalDateTime.now(),
                        gyldigTil = null,
                        periodeGjortUgyldigAvVedtaksid = null,
                        beløp = BigDecimal.valueOf(1670),
                        valutakode = valutakode,
                        resultatkode = "KBB",
                    ),
                ),
        )

    private fun sluttberegning(valutakode: Valutakode = Valutakode.NOK) =
        GrunnlagDto(
            referanse = "sluttberegning_indeksregulering_ref",
            type = Grunnlagstype.SLUTTBEREGNING_INDEKSREGULERING,
            innhold =
                POJONode(
                    SluttberegningIndeksregulering(
                        periode = ÅrMånedsperiode(YearMonth.of(2026, 7), null),
                        beløp = Beløp(verdi = BigDecimal.valueOf(1730), valutakode = valutakode),
                        originaltBeløp = Beløp(verdi = BigDecimal.valueOf(1670), valutakode = valutakode),
                        nesteIndeksreguleringsår = Year.of(2027),
                    ),
                ),
        )

    @BeforeEach
    fun setup() {
        every { personConsumer.hentPerson(any()) } answers {
            PersonDto(ident = firstArg(), fødselsdato = LocalDate.of(1990, 1, 1))
        }
        every { vedtakMapper.reellMottakerEllerBidragsmottaker(any(), any()) } returns Personident(mottaker)
        every { vedtakService.finnSisteVedtaksid(any()) } returns 10
    }

    @Test
    fun `skal sette FEILET når skyldner mangler`() {
        val indeksregulering = indeksregulering(barn(skyldner = null))

        val resultat = service.gjennomførIndeksregulering(indeksregulering, simuler = false)

        resultat.status shouldBe Status.FEILET
        resultat.behandlingstype shouldBe Behandlingstype.FEILET
        resultat.begrunnelse shouldContain "MANGLER_SKYLDNER"
    }

    @Test
    fun `skal sette INGEN når det ikke finnes løpende stønad`() {
        every { beløpshistorikkConsumer.hentLøpendeStønad(any()) } returns null

        val resultat = service.gjennomførIndeksregulering(indeksregulering(), simuler = false)

        resultat.status shouldBe Status.BEHANDLET
        resultat.behandlingstype shouldBe Behandlingstype.INGEN
        resultat.begrunnelse shouldContain "INGEN_LØPENDE_STØNAD"
    }

    @Test
    fun `skal sette MANUELL når stønaden løper i utenlandsk valuta`() {
        every { beløpshistorikkConsumer.hentLøpendeStønad(any()) } returns stønad(valutakode = "SEK")

        val resultat = service.gjennomførIndeksregulering(indeksregulering(), simuler = false)

        resultat.status shouldBe Status.BEHANDLET
        resultat.behandlingstype shouldBe Behandlingstype.MANUELL
        resultat.begrunnelse shouldContain "LØPER_I_UTENLANDSK_VALUTA"
        verify(exactly = 0) { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(any()) }
    }

    @Test
    fun `skal sette MANUELL når beregnet beløp er i utenlandsk valuta`() {
        every { beløpshistorikkConsumer.hentLøpendeStønad(any()) } returns stønad()
        every { beregnIndeksreguleringApi.beregnIndeksregulering(any()) } returns listOf(sluttberegning(Valutakode.SEK))

        val resultat = service.gjennomførIndeksregulering(indeksregulering(), simuler = false)

        resultat.status shouldBe Status.BEHANDLET
        resultat.behandlingstype shouldBe Behandlingstype.MANUELL
        resultat.begrunnelse shouldContain "LØPER_I_UTENLANDSK_VALUTA"
        verify(exactly = 0) { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(any()) }
    }

    @Test
    fun `skal beregne indeksregulering og opprette vedtaksforslag`() {
        val indeksregulering = indeksregulering()
        every { beløpshistorikkConsumer.hentLøpendeStønad(any()) } returns stønad()
        every { beregnIndeksreguleringApi.beregnIndeksregulering(any()) } returns listOf(sluttberegning())
        every { vedtakMapper.hentBarn(any(), any()) } returns mockk()
        val requestSlot = slot<OpprettVedtakRequestDto>()
        every { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(capture(requestSlot)) } returns 999

        val resultat = service.gjennomførIndeksregulering(indeksregulering, simuler = false)

        resultat.status shouldBe Status.BEHANDLET
        resultat.behandlingstype shouldBe Behandlingstype.FATTET_FORSLAG
        resultat.vedtak shouldBe 999
        resultat.beløp shouldBe BigDecimal.valueOf(1730)

        val request = requestSlot.captured
        request.type shouldBe Vedtakstype.INDEKSREGULERING
        request.kilde shouldBe Vedtakskilde.AUTOMATISK
        request.unikReferanse shouldBe indeksregulering.unikReferanse
        val stønadsendring = request.stønadsendringListe.first()
        stønadsendring.kravhaver shouldBe Personident(kravhaver)
        stønadsendring.skyldner shouldBe Personident(skyldner)
        stønadsendring.mottaker shouldBe Personident(mottaker)
        stønadsendring.sisteVedtaksid shouldBe 10
        stønadsendring.førsteIndeksreguleringsår shouldBe 2027
        val periode = stønadsendring.periodeListe.first()
        periode.beløp shouldBe BigDecimal.valueOf(1730)
        periode.valutakode shouldBe "NOK"
        periode.periode.fom shouldBe YearMonth.of(2026, 7)
        request.grunnlagListe.find { it.type == Grunnlagstype.SLUTTBEREGNING_INDEKSREGULERING }.shouldNotBeNull()
    }

    @Test
    fun `skal ikke opprette vedtaksforslag ved simulering`() {
        every { beløpshistorikkConsumer.hentLøpendeStønad(any()) } returns stønad()
        every { beregnIndeksreguleringApi.beregnIndeksregulering(any()) } returns listOf(sluttberegning())
        every { vedtakMapper.hentBarn(any(), any()) } returns mockk()

        val resultat = service.gjennomførIndeksregulering(indeksregulering(), simuler = true)

        resultat.status shouldBe Status.SIMULERT
        resultat.behandlingstype shouldBe Behandlingstype.FATTET_FORSLAG
        resultat.vedtak shouldBe null
        resultat.beløp shouldBe BigDecimal.valueOf(1730)
        verify(exactly = 0) { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(any()) }
    }
}
