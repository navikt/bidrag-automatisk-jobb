package no.nav.bidrag.automatiskjobb.service

import com.fasterxml.jackson.databind.node.POJONode
import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkConstructor
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.consumer.BidragStønadConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.testdata.opprettBostatatusperiode
import no.nav.bidrag.automatiskjobb.testdata.opprettDelberegningBarnIHusstand
import no.nav.bidrag.automatiskjobb.testdata.opprettDelberegningSumInntekt
import no.nav.bidrag.automatiskjobb.testdata.opprettEngangsbeløpSærbidrag
import no.nav.bidrag.automatiskjobb.testdata.opprettGrunnlagDelberegningAndel
import no.nav.bidrag.automatiskjobb.testdata.opprettGrunnlagSluttberegningBidrag
import no.nav.bidrag.automatiskjobb.testdata.opprettGrunnlagSluttberegningForskudd
import no.nav.bidrag.automatiskjobb.testdata.opprettGrunnlagSluttberegningSærbidrag
import no.nav.bidrag.automatiskjobb.testdata.opprettGrunnlagSøknad
import no.nav.bidrag.automatiskjobb.testdata.opprettInntektsrapportering
import no.nav.bidrag.automatiskjobb.testdata.opprettSivilstandPeriode
import no.nav.bidrag.automatiskjobb.testdata.opprettStønadDto
import no.nav.bidrag.automatiskjobb.testdata.opprettStønadPeriodeDto
import no.nav.bidrag.automatiskjobb.testdata.opprettStønadsendringBidrag
import no.nav.bidrag.automatiskjobb.testdata.opprettStønadsendringForskudd
import no.nav.bidrag.automatiskjobb.testdata.opprettVedtakDto
import no.nav.bidrag.automatiskjobb.testdata.opprettVedtakForStønad
import no.nav.bidrag.automatiskjobb.testdata.opprettVedtakhendelse
import no.nav.bidrag.automatiskjobb.testdata.personIdentBidragsmottaker
import no.nav.bidrag.automatiskjobb.testdata.personIdentBidragspliktig
import no.nav.bidrag.automatiskjobb.testdata.personIdentSøknadsbarn1
import no.nav.bidrag.automatiskjobb.testdata.persongrunnlagBA
import no.nav.bidrag.automatiskjobb.testdata.persongrunnlagBM
import no.nav.bidrag.automatiskjobb.testdata.persongrunnlagBP
import no.nav.bidrag.automatiskjobb.testdata.saksnummer
import no.nav.bidrag.beregn.forskudd.BeregnForskuddApi
import no.nav.bidrag.beregn.vedtak.Vedtaksfiltrering
import no.nav.bidrag.commons.web.mock.stubSjablonProvider
import no.nav.bidrag.commons.web.mock.stubSjablonService
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSumInntekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.vedtak.response.HentVedtakForStønadResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

val vedtaksidBidrag = 2
val vedtaksidForskudd = 1
val vedtaksidForskudd2 = 4
val vedtaksidSærbidrag = 3

@ExtendWith(MockKExtension::class)
class RevurderForskuddServiceTest {
    lateinit var service: RevurderForskuddService

    @MockK
    lateinit var bidragStønadConsumer: BidragStønadConsumer

    @MockK
    lateinit var bidragVedtakConsumer: BidragVedtakConsumer

    val unleash = FakeUnleash()

    @BeforeEach
    fun initMocks() {
        // commonObjectmapper.readValue(hentFil("/__files/vedtak_forskudd.json"))

        stubSjablonService()
        stubSjablonProvider()
        service = RevurderForskuddService(bidragStønadConsumer, bidragVedtakConsumer, BeregnForskuddApi(), Vedtaksfiltrering())
    }

    @Test
    fun `skal returnere at forskudd er redusert når beregnet forksudd er lavere enn løpende etter bidrag vedtak`() {
        every { bidragStønadConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) } returns opprettForskuddVedtakRespons()
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidBidrag)) } returns
            opprettVedtakDto().copy(
                grunnlagListe = opprettGrunnlagslisteBidrag(),
                stønadsendringListe =
                    listOf(
                        opprettStønadsendringBidrag(),
                    ),
            )
        val beregnCapture = mutableListOf<BeregnGrunnlag>()
        mockkConstructor(BeregnForskuddApi::class)
        every { BeregnForskuddApi().beregn(capture(beregnCapture)) } answers { callOriginal() }
        val resultat = service.erForskuddRedusert(opprettVedtakhendelse(vedtaksidBidrag))
        resultat.shouldHaveSize(1)
        resultat.first().gjelderBarn shouldBe personIdentSøknadsbarn1
        resultat.first().bidragsmottaker shouldBe personIdentBidragsmottaker
        resultat.first().saksnummer shouldBe saksnummer
    }

    @Test
    fun `skal beregne forskudd når bostatus barn referanse er gjelderReferanse`() {
        every { bidragStønadConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) } returns
            opprettVedtakDto().copy(
                grunnlagListe =
                    listOf(
                        persongrunnlagBA,
                        persongrunnlagBM,
                        opprettGrunnlagSluttberegningForskudd(),
                        opprettInntektsrapportering(),
                        opprettSivilstandPeriode(),
                        opprettGrunnlagSøknad(),
                        opprettBostatatusperiode().copy(
                            gjelderBarnReferanse = null,
                            gjelderReferanse = persongrunnlagBA.referanse,
                        ),
                        opprettDelberegningBarnIHusstand(),
                        opprettDelberegningSumInntekt(),
                    ),
                stønadsendringListe =
                    listOf(
                        opprettStønadsendringForskudd(),
                    ),
            )

        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidBidrag)) } returns
            opprettVedtakDto().copy(
                grunnlagListe = opprettGrunnlagslisteBidrag(),
                stønadsendringListe =
                    listOf(
                        opprettStønadsendringBidrag(),
                    ),
            )
        val beregnCapture = mutableListOf<BeregnGrunnlag>()
        mockkConstructor(BeregnForskuddApi::class)
        every { BeregnForskuddApi().beregn(capture(beregnCapture)) } answers { callOriginal() }
        val resultat = service.erForskuddRedusert(opprettVedtakhendelse(vedtaksidBidrag))
        resultat.shouldHaveSize(1)
        resultat.first().gjelderBarn shouldBe personIdentSøknadsbarn1
        resultat.first().bidragsmottaker shouldBe personIdentBidragsmottaker
        resultat.first().saksnummer shouldBe saksnummer
    }

    @Test
    fun `skal ignorere forskudd hvis siste periode i forskudd slutter før dagens dato`() {
        every { bidragStønadConsumer.hentHistoriskeStønader(any()) } returns
            opprettStønadDto(
                stønadstype = Stønadstype.FORSKUDD,
                periodeListe =
                    listOf(
                        opprettStønadPeriodeDto(
                            ÅrMånedsperiode(LocalDate.now().minusMonths(4), LocalDate.now().minusMonths(2)),
                            beløp = BigDecimal("5600"),
                            vedtakId = vedtaksidForskudd,
                        ),
                    ),
            )
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidBidrag)) } returns
            opprettVedtakDto().copy(
                grunnlagListe = opprettGrunnlagslisteBidrag(),
                stønadsendringListe =
                    listOf(
                        opprettStønadsendringBidrag().copy(type = Stønadstype.BIDRAG18AAR),
                    ),
            )
        val beregnCapture = mutableListOf<BeregnGrunnlag>()
        mockkConstructor(BeregnForskuddApi::class)
        every { BeregnForskuddApi().beregn(capture(beregnCapture)) } answers { callOriginal() }
        val resultat = service.erForskuddRedusert(opprettVedtakhendelse(vedtaksidBidrag))
        resultat.shouldHaveSize(0)
        verify(exactly = 0) { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) }
    }

    @Test
    fun `skal returnere at forskudd er redusert når beregnet forksudd er lavere enn løpende etter bidrag 18 år vedtak`() {
        every { bidragStønadConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) } returns opprettForskuddVedtakRespons()

        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidBidrag)) } returns
            opprettVedtakDto().copy(
                grunnlagListe = opprettGrunnlagslisteBidrag(),
                stønadsendringListe =
                    listOf(
                        opprettStønadsendringBidrag().copy(type = Stønadstype.BIDRAG18AAR),
                    ),
            )
        val beregnCapture = mutableListOf<BeregnGrunnlag>()
        mockkConstructor(BeregnForskuddApi::class)
        every { BeregnForskuddApi().beregn(capture(beregnCapture)) } answers { callOriginal() }
        val resultat = service.erForskuddRedusert(opprettVedtakhendelse(vedtaksidBidrag, stonadType = Stønadstype.BIDRAG18AAR))
        resultat.shouldHaveSize(1)
        resultat.first().gjelderBarn shouldBe personIdentSøknadsbarn1
        resultat.first().bidragsmottaker shouldBe personIdentBidragsmottaker
        resultat.first().saksnummer shouldBe saksnummer
    }

    @Test
    fun `skal hente siste manuelle vedtak hvis siste periode for forskudd er indeksregulering`() {
        every { bidragStønadConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) } returns
            opprettVedtakDto().copy(
                type = Vedtakstype.INDEKSREGULERING,
            )
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd2)) } returns opprettForskuddVedtakRespons()

        every { bidragVedtakConsumer.hentVedtakForStønad(any()) } returns
            HentVedtakForStønadResponse(
                vedtakListe =
                    listOf(
                        opprettVedtakForStønad(personIdentBidragspliktig, stønadstype = Stønadstype.FORSKUDD).copy(
                            vedtaksid = 55,
                            type = Vedtakstype.ALDERSJUSTERING,
                        ),
                        opprettVedtakForStønad(personIdentBidragspliktig, stønadstype = Stønadstype.FORSKUDD).copy(
                            vedtaksid = vedtaksidForskudd2.toLong(),
                            type = Vedtakstype.ENDRING,
                        ),
                    ),
            )
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidBidrag)) } returns
            opprettVedtakDto().copy(
                grunnlagListe = opprettGrunnlagslisteBidrag(),
                stønadsendringListe =
                    listOf(
                        opprettStønadsendringBidrag(),
                    ),
            )
        val beregnCapture = mutableListOf<BeregnGrunnlag>()
        mockkConstructor(BeregnForskuddApi::class)
        every { BeregnForskuddApi().beregn(capture(beregnCapture)) } answers { callOriginal() }
        val resultat = service.erForskuddRedusert(opprettVedtakhendelse(vedtaksidBidrag))
        resultat.shouldHaveSize(1)
        resultat.first().gjelderBarn shouldBe personIdentSøknadsbarn1
        resultat.first().bidragsmottaker shouldBe personIdentBidragsmottaker
        resultat.first().saksnummer shouldBe saksnummer

        verify(exactly = 1) {
            bidragVedtakConsumer.hentVedtakForStønad(
                withArg {
                    it.sak shouldBe Saksnummer(saksnummer)
                    it.type shouldBe Stønadstype.FORSKUDD
                    it.skyldner shouldBe skyldnerNav
                    it.kravhaver shouldBe Personident(personIdentSøknadsbarn1)
                },
            )
        }
    }

    @Test
    fun `skal returnere tom liste hvis forskudd ikke er redusert`() {
        every { bidragStønadConsumer.hentHistoriskeStønader(any()) } returns
            opprettStønadDto(
                stønadstype = Stønadstype.FORSKUDD,
                periodeListe =
                    listOf(
                        opprettStønadPeriodeDto(
                            ÅrMånedsperiode(LocalDate.now().minusMonths(4), null),
                            beløp = BigDecimal("2100"),
                            vedtakId = vedtaksidForskudd,
                        ),
                    ),
            )
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) } returns
            opprettVedtakDto().copy(
                grunnlagListe =
                    listOf(
                        persongrunnlagBA,
                        persongrunnlagBM,
                        opprettGrunnlagSluttberegningForskudd(),
                        opprettInntektsrapportering(),
                        opprettSivilstandPeriode(),
                        opprettGrunnlagSøknad(),
                        opprettBostatatusperiode(),
                        opprettDelberegningBarnIHusstand(),
                        opprettDelberegningSumInntekt(),
                    ),
                stønadsendringListe =
                    listOf(
                        opprettStønadsendringForskudd(),
                    ),
            )
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidBidrag)) } returns
            opprettVedtakDto().copy(
                grunnlagListe =
                    listOf(
                        persongrunnlagBA,
                        persongrunnlagBM,
                        persongrunnlagBP,
                        opprettGrunnlagSøknad(),
                        opprettGrunnlagSluttberegningBidrag(),
                        opprettInntektsrapportering().copy(
                            innhold =
                                POJONode(
                                    InntektsrapporteringPeriode(
                                        periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                        beløp = BigDecimal(300000),
                                        manueltRegistrert = true,
                                        inntektsrapportering = Inntektsrapportering.LØNN_MANUELT_BEREGNET,
                                        valgt = true,
                                    ),
                                ),
                        ),
                        opprettGrunnlagDelberegningAndel(),
                        opprettDelberegningSumInntekt(),
                    ),
                stønadsendringListe =
                    listOf(
                        opprettStønadsendringBidrag(),
                    ),
            )
        val beregnCapture = mutableListOf<BeregnGrunnlag>()
        mockkConstructor(BeregnForskuddApi::class)
        every { BeregnForskuddApi().beregn(capture(beregnCapture)) } answers { callOriginal() }
        val resultat = service.erForskuddRedusert(opprettVedtakhendelse(vedtaksidBidrag))
        resultat.shouldHaveSize(0)

        verify(exactly = 1) { bidragVedtakConsumer.hentVedtak(eq(vedtaksidBidrag)) }
        verify(exactly = 1) { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) }
        verify(exactly = 1) { bidragStønadConsumer.hentHistoriskeStønader(any()) }
    }

    @Test
    fun `skal returnere at forskudd er redusert når beregnet forksudd er lavere enn løpende etter særbidrag vedtak`() {
        every { bidragStønadConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) } returns opprettForskuddVedtakRespons()

        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidSærbidrag)) } returns
            opprettVedtakDto().copy(
                grunnlagListe =
                    listOf(
                        persongrunnlagBA,
                        persongrunnlagBM,
                        persongrunnlagBP,
                        opprettGrunnlagSøknad(),
                        opprettGrunnlagSluttberegningSærbidrag(),
                        opprettInntektsrapportering(),
                        opprettGrunnlagDelberegningAndel(),
                        opprettDelberegningSumInntekt(),
                    ),
                engangsbeløpListe =
                    listOf(
                        opprettEngangsbeløpSærbidrag(),
                    ),
            )
        val beregnCapture = mutableListOf<BeregnGrunnlag>()
        mockkConstructor(BeregnForskuddApi::class)
        every { BeregnForskuddApi().beregn(capture(beregnCapture)) } answers { callOriginal() }
        val resultat = service.erForskuddRedusert(opprettVedtakhendelse(vedtaksidSærbidrag))
        resultat.shouldHaveSize(1)
        resultat.first().gjelderBarn shouldBe personIdentSøknadsbarn1
        resultat.first().bidragsmottaker shouldBe personIdentBidragsmottaker
        resultat.first().saksnummer shouldBe saksnummer

        verify(exactly = 1) { bidragVedtakConsumer.hentVedtak(eq(vedtaksidSærbidrag)) }
        verify(exactly = 1) { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) }
        verify(exactly = 1) { bidragStønadConsumer.hentHistoriskeStønader(any()) }
    }

    @Test
    fun `skal ignorere fattet vedtak hvis grunnlagslisten er tom`() {
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidBidrag)) } returns
            opprettVedtakDto().copy(
                grunnlagListe = emptyList(),
                stønadsendringListe =
                    listOf(
                        opprettStønadsendringBidrag().copy(type = Stønadstype.BIDRAG),
                    ),
            )
        val beregnCapture = mutableListOf<BeregnGrunnlag>()
        mockkConstructor(BeregnForskuddApi::class)
        every { BeregnForskuddApi().beregn(capture(beregnCapture)) } answers { callOriginal() }
        val resultat = service.erForskuddRedusert(opprettVedtakhendelse(vedtaksidBidrag))
        resultat.shouldHaveSize(0)
        verify(exactly = 0) { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) }
        verify(exactly = 0) { bidragStønadConsumer.hentHistoriskeStønader(any()) }
    }

    @Test
    fun `skal ignorere forksudd vedtak hvis grunnlagslisten er tom`() {
        every { bidragStønadConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) } returns
            opprettVedtakDto().copy(
                grunnlagListe = emptyList(),
                stønadsendringListe =
                    listOf(
                        opprettStønadsendringForskudd(),
                    ),
            )
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidSærbidrag)) } returns
            opprettVedtakDto().copy(
                grunnlagListe =
                    listOf(
                        persongrunnlagBA,
                        persongrunnlagBM,
                        persongrunnlagBP,
                        opprettGrunnlagSøknad(),
                        opprettGrunnlagSluttberegningSærbidrag(),
                        opprettInntektsrapportering(),
                        opprettGrunnlagDelberegningAndel(),
                        opprettDelberegningSumInntekt(),
                    ),
                engangsbeløpListe =
                    listOf(
                        opprettEngangsbeløpSærbidrag(),
                    ),
            )
        val beregnCapture = mutableListOf<BeregnGrunnlag>()
        mockkConstructor(BeregnForskuddApi::class)
        every { BeregnForskuddApi().beregn(capture(beregnCapture)) } answers { callOriginal() }
        val resultat = service.erForskuddRedusert(opprettVedtakhendelse(vedtaksidSærbidrag))
        resultat.shouldHaveSize(0)
        verify(exactly = 1) { bidragVedtakConsumer.hentVedtak(eq(vedtaksidSærbidrag)) }
        verify(exactly = 1) { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) }
        verify(exactly = 1) { bidragStønadConsumer.hentHistoriskeStønader(any()) }
    }
}

private fun opprettForskuddVedtakRespons() =
    opprettVedtakDto().copy(
        grunnlagListe =
            listOf(
                persongrunnlagBA,
                persongrunnlagBM,
                opprettGrunnlagSluttberegningForskudd(),
                opprettInntektsrapportering().copy(
                    innhold =
                        POJONode(
                            InntektsrapporteringPeriode(
                                periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                beløp = BigDecimal(300000),
                                manueltRegistrert = true,
                                inntektsrapportering = Inntektsrapportering.LØNN_MANUELT_BEREGNET,
                                valgt = true,
                            ),
                        ),
                ),
                opprettSivilstandPeriode(),
                opprettGrunnlagSøknad(),
                opprettBostatatusperiode(),
                opprettDelberegningBarnIHusstand(),
                opprettDelberegningSumInntekt().copy(
                    innhold =
                        POJONode(
                            DelberegningSumInntekt(
                                periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                totalinntekt = BigDecimal(300000),
                            ),
                        ),
                ),
            ),
        stønadsendringListe =
            listOf(
                opprettStønadsendringForskudd(),
            ),
    )

private fun opprettGrunnlagslisteBidrag() =
    listOf(
        persongrunnlagBA,
        persongrunnlagBM,
        persongrunnlagBP,
        opprettGrunnlagSøknad(),
        opprettGrunnlagSluttberegningBidrag(),
        opprettInntektsrapportering(),
        opprettGrunnlagDelberegningAndel(),
        opprettDelberegningSumInntekt(),
    )

private fun opprettLøpendeForskuddRespons() =
    opprettStønadDto(
        stønadstype = Stønadstype.FORSKUDD,
        periodeListe =
            listOf(
                opprettStønadPeriodeDto(
                    ÅrMånedsperiode(LocalDate.now().minusMonths(4), null),
                    beløp = BigDecimal("5600"),
                    vedtakId = vedtaksidForskudd,
                ),
            ),
    )
