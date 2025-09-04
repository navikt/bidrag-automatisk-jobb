package no.nav.bidrag.automatiskjobb.service.revurderforskudd

import com.fasterxml.jackson.databind.node.POJONode
import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkConstructor
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.service.RevurderForskuddService
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
import no.nav.bidrag.automatiskjobb.testdata.opprettSakRespons
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
import no.nav.bidrag.automatiskjobb.testdata.personIdentSøknadsbarn2
import no.nav.bidrag.automatiskjobb.testdata.persongrunnlagBA
import no.nav.bidrag.automatiskjobb.testdata.persongrunnlagBA2
import no.nav.bidrag.automatiskjobb.testdata.persongrunnlagBP
import no.nav.bidrag.automatiskjobb.testdata.saksnummer
import no.nav.bidrag.automatiskjobb.testdata.testdataBidragsmottaker
import no.nav.bidrag.beregn.forskudd.BeregnForskuddApi
import no.nav.bidrag.beregn.vedtak.Vedtaksfiltrering
import no.nav.bidrag.commons.web.mock.stubSjablonProvider
import no.nav.bidrag.commons.web.mock.stubSjablonService
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.felles.personidentNav
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSumInntekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.vedtak.response.HentVedtakForStønadResponse
import no.nav.bidrag.transport.sak.RolleDto
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
    lateinit var bidragBeløpshistorikkConsumer: BidragBeløpshistorikkConsumer

    @MockK
    lateinit var bidragVedtakConsumer: BidragVedtakConsumer

    @MockK
    lateinit var bidragSakConsumer: BidragSakConsumer

    @MockK
    lateinit var bidragPersonConsumer: BidragPersonConsumer

    val unleash = FakeUnleash()

    @BeforeEach
    fun initMocks() {
        // commonObjectmapper.readValue(hentFil("/__files/vedtak_forskudd.json"))

        every { bidragSakConsumer.hentSak(any()) } returns opprettSakRespons()
        stubSjablonService()
        stubSjablonProvider()
        service =
            RevurderForskuddService(
                bidragBeløpshistorikkConsumer,
                bidragVedtakConsumer,
                bidragSakConsumer,
                bidragPersonConsumer,
                BeregnForskuddApi(),
                Vedtaksfiltrering(),
            )
    }

    @Test
    fun `skal returnere at forskudd er redusert når beregnet forksudd er lavere enn løpende etter bidrag vedtak`() {
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
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
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) } returns
            opprettVedtakDto().copy(
                grunnlagListe =
                    listOf(
                        persongrunnlagBA,
                        testdataBidragsmottaker.tilGrunnlag(),
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
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns
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
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
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
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) } returns
            opprettVedtakDto().copy(
                type = Vedtakstype.INDEKSREGULERING,
                kilde = Vedtakskilde.AUTOMATISK,
            )
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd2)) } returns opprettForskuddVedtakRespons()

        every { bidragVedtakConsumer.hentVedtakForStønad(any()) } returns
            HentVedtakForStønadResponse(
                vedtakListe =
                    listOf(
                        opprettVedtakForStønad(personIdentBidragspliktig, stønadstype = Stønadstype.FORSKUDD).copy(
                            vedtaksid = 55,
                            type = Vedtakstype.ALDERSJUSTERING,
                            kilde = Vedtakskilde.AUTOMATISK,
                        ),
                        opprettVedtakForStønad(personIdentBidragspliktig, stønadstype = Stønadstype.FORSKUDD).copy(
                            vedtaksid = vedtaksidForskudd2,
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
                    it.skyldner shouldBe personidentNav
                    it.kravhaver shouldBe Personident(personIdentSøknadsbarn1)
                },
            )
        }
    }

    @Test
    fun `skal returnere tom liste hvis forskudd ikke er redusert`() {
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns
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
                        testdataBidragsmottaker.tilGrunnlag(),
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
                        testdataBidragsmottaker.tilGrunnlag(),
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
        verify(exactly = 1) { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) }
    }

    @Test
    fun `skal returnere at forskudd er redusert når beregnet forksudd er lavere enn løpende etter særbidrag vedtak`() {
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) } returns opprettForskuddVedtakRespons()

        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidSærbidrag)) } returns
            opprettVedtakDto().copy(
                grunnlagListe =
                    listOf(
                        persongrunnlagBA,
                        testdataBidragsmottaker.tilGrunnlag(),
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
        verify(exactly = 1) { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) }
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
        verify(exactly = 0) { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) }
    }

    @Test
    fun `skal ignorere forksudd vedtak hvis grunnlagslisten er tom`() {
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
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
                        testdataBidragsmottaker.tilGrunnlag(),
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
        verify(exactly = 1) { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) }
    }

    @Test
    fun `skal returnere at forskudd er redusert når beregnet forksudd er lavere enn løpende etter bidrag vedtak for annen barn i saken`() {
        every {
            bidragBeløpshistorikkConsumer.hentHistoriskeStønader(
                match {
                    it.kravhaver == Personident(personIdentSøknadsbarn1)
                },
            )
        } returns
            opprettLøpendeForskuddRespons(vedtaksidForskudd, BigDecimal("1460"))
        every {
            bidragBeløpshistorikkConsumer.hentHistoriskeStønader(
                match {
                    it.kravhaver == Personident(personIdentSøknadsbarn2)
                },
            )
        } returns opprettLøpendeForskuddRespons(vedtaksidForskudd2)
        every { bidragSakConsumer.hentSak(any()) } returns
            opprettSakRespons()
                .copy(
                    roller =
                        listOf(
                            RolleDto(
                                Personident(personIdentBidragsmottaker),
                                type = Rolletype.BIDRAGSMOTTAKER,
                            ),
                            RolleDto(
                                Personident(personIdentBidragspliktig),
                                type = Rolletype.BIDRAGSPLIKTIG,
                            ),
                            RolleDto(
                                Personident(personIdentSøknadsbarn2),
                                type = Rolletype.BARN,
                            ),
                            RolleDto(
                                Personident(personIdentSøknadsbarn1),
                                type = Rolletype.BARN,
                            ),
                        ),
                )
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) } returns
            opprettForskuddVedtakRespons(
                søknadsbarn = persongrunnlagBA,
                listOf(
                    opprettDelberegningSumInntekt().copy(
                        grunnlagsreferanseListe = listOf("INNTEKT_MANUEL", "INNTEKT_BARNETILLEGG", "INNTEKT_KONTANTSTØTTE"),
                        innhold =
                            POJONode(
                                DelberegningSumInntekt(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    totalinntekt = BigDecimal(290000),
                                ),
                            ),
                    ),
                    opprettInntektsrapportering().copy(
                        referanse = "INNTEKT_KONTANTSTØTTE",
                        innhold =
                            POJONode(
                                InntektsrapporteringPeriode(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    beløp = BigDecimal(20000),
                                    manueltRegistrert = true,
                                    inntektsrapportering = Inntektsrapportering.KONTANTSTØTTE,
                                    valgt = true,
                                ),
                            ),
                        gjelderBarnReferanse = persongrunnlagBA.referanse,
                    ),
                    opprettInntektsrapportering().copy(
                        referanse = "INNTEKT_BARNETILLEGG",
                        innhold =
                            POJONode(
                                InntektsrapporteringPeriode(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    beløp = BigDecimal(20000),
                                    manueltRegistrert = true,
                                    inntektsrapportering = Inntektsrapportering.BARNETILLEGG,
                                    valgt = true,
                                ),
                            ),
                        gjelderBarnReferanse = persongrunnlagBA.referanse,
                    ),
                    opprettInntektsrapportering().copy(
                        referanse = "INNTEKT_MANUEL",
                        innhold =
                            POJONode(
                                InntektsrapporteringPeriode(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    beløp = BigDecimal(25000),
                                    manueltRegistrert = true,
                                    inntektsrapportering = Inntektsrapportering.LØNN_MANUELT_BEREGNET,
                                    valgt = true,
                                ),
                            ),
                    ),
                ),
            )
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd2)) } returns
            opprettForskuddVedtakRespons(
                søknadsbarn = persongrunnlagBA2,
                listOf(
                    opprettDelberegningSumInntekt().copy(
                        grunnlagsreferanseListe = listOf("INNTEKT_MANUEL", "INNTEKT_BARNETILLEGG", "INNTEKT_KONTANTSTØTTE"),
                        innhold =
                            POJONode(
                                DelberegningSumInntekt(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    totalinntekt = BigDecimal(270000),
                                ),
                            ),
                    ),
                    opprettInntektsrapportering().copy(
                        referanse = "INNTEKT_KONTANTSTØTTE",
                        innhold =
                            POJONode(
                                InntektsrapporteringPeriode(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    beløp = BigDecimal(10000),
                                    manueltRegistrert = true,
                                    inntektsrapportering = Inntektsrapportering.KONTANTSTØTTE,
                                    valgt = true,
                                ),
                            ),
                        gjelderBarnReferanse = persongrunnlagBA2.referanse,
                    ),
                    opprettInntektsrapportering().copy(
                        referanse = "INNTEKT_BARNETILLEGG",
                        innhold =
                            POJONode(
                                InntektsrapporteringPeriode(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    beløp = BigDecimal(10000),
                                    manueltRegistrert = true,
                                    inntektsrapportering = Inntektsrapportering.BARNETILLEGG,
                                    valgt = true,
                                ),
                            ),
                        gjelderBarnReferanse = persongrunnlagBA2.referanse,
                    ),
                    opprettInntektsrapportering().copy(
                        referanse = "INNTEKT_MANUEL",
                        innhold =
                            POJONode(
                                InntektsrapporteringPeriode(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    beløp = BigDecimal(25000),
                                    manueltRegistrert = true,
                                    inntektsrapportering = Inntektsrapportering.LØNN_MANUELT_BEREGNET,
                                    valgt = true,
                                ),
                            ),
                    ),
                ),
            )

        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidBidrag)) } returns
            opprettVedtakDto().copy(
                grunnlagListe =
                    opprettGrunnlagslisteBidrag(
                        listOf(
                            opprettDelberegningSumInntekt().copy(
                                grunnlagsreferanseListe = listOf("INNTEKT_MANUEL", "INNTEKT_BARNETILLEGG", "INNTEKT_KONTANTSTØTTE"),
                                innhold =
                                    POJONode(
                                        DelberegningSumInntekt(
                                            periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                            totalinntekt = BigDecimal(370000),
                                        ),
                                    ),
                            ),
                            opprettInntektsrapportering().copy(
                                referanse = "INNTEKT_KONTANTSTØTTE",
                                innhold =
                                    POJONode(
                                        InntektsrapporteringPeriode(
                                            periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                            beløp = BigDecimal(10000),
                                            manueltRegistrert = true,
                                            inntektsrapportering = Inntektsrapportering.KONTANTSTØTTE,
                                            valgt = true,
                                        ),
                                    ),
                                gjelderBarnReferanse = persongrunnlagBA.referanse,
                            ),
                            opprettInntektsrapportering().copy(
                                referanse = "INNTEKT_BARNETILLEGG",
                                innhold =
                                    POJONode(
                                        InntektsrapporteringPeriode(
                                            periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                            beløp = BigDecimal(10000),
                                            manueltRegistrert = true,
                                            inntektsrapportering = Inntektsrapportering.BARNETILLEGG,
                                            valgt = true,
                                        ),
                                    ),
                                gjelderBarnReferanse = persongrunnlagBA.referanse,
                            ),
                            opprettInntektsrapportering().copy(
                                referanse = "INNTEKT_MANUEL",
                                innhold =
                                    POJONode(
                                        InntektsrapporteringPeriode(
                                            periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                            beløp = BigDecimal(35000),
                                            manueltRegistrert = true,
                                            inntektsrapportering = Inntektsrapportering.LØNN_MANUELT_BEREGNET,
                                            valgt = true,
                                        ),
                                    ),
                            ),
                        ),
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
        resultat.shouldHaveSize(1)
        resultat.first().gjelderBarn shouldBe personIdentSøknadsbarn2
        resultat.first().bidragsmottaker shouldBe personIdentBidragsmottaker
        resultat.first().saksnummer shouldBe saksnummer
        resultat.first().stønadstype shouldBe Stønadstype.BIDRAG

        verify(exactly = 1) { bidragVedtakConsumer.hentVedtak(vedtaksidForskudd) }
        verify(exactly = 1) { bidragVedtakConsumer.hentVedtak(vedtaksidForskudd) }
        verify(exactly = 1) { bidragSakConsumer.hentSak(saksnummer) }
        verify(exactly = 1) {
            bidragBeløpshistorikkConsumer.hentHistoriskeStønader(
                withArg {
                    it.kravhaver shouldBe Personident(personIdentSøknadsbarn2)
                },
            )
        }
        verify(exactly = 1) {
            bidragBeløpshistorikkConsumer.hentHistoriskeStønader(
                withArg {
                    it.kravhaver shouldBe Personident(personIdentSøknadsbarn1)
                },
            )
        }
    }

    @Test
    fun `skal returnere at forskudd er redusert når beregnet forksudd er lavere enn løpende etter bidrag vedtak for flere barn i saken`() {
        every {
            bidragBeløpshistorikkConsumer.hentHistoriskeStønader(
                match {
                    it.kravhaver == Personident(personIdentSøknadsbarn1)
                },
            )
        } returns opprettLøpendeForskuddRespons(vedtaksidForskudd)
        every {
            bidragBeløpshistorikkConsumer.hentHistoriskeStønader(
                match {
                    it.kravhaver == Personident(personIdentSøknadsbarn2)
                },
            )
        } returns opprettLøpendeForskuddRespons(vedtaksidForskudd2)
        every { bidragSakConsumer.hentSak(any()) } returns
            opprettSakRespons()
                .copy(
                    roller =
                        listOf(
                            RolleDto(
                                Personident(personIdentBidragsmottaker),
                                type = Rolletype.BIDRAGSMOTTAKER,
                            ),
                            RolleDto(
                                Personident(personIdentBidragspliktig),
                                type = Rolletype.BIDRAGSPLIKTIG,
                            ),
                            RolleDto(
                                Personident(personIdentSøknadsbarn2),
                                type = Rolletype.BARN,
                            ),
                            RolleDto(
                                Personident(personIdentSøknadsbarn1),
                                type = Rolletype.BARN,
                            ),
                        ),
                )
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd)) } returns
            opprettForskuddVedtakRespons(
                søknadsbarn = persongrunnlagBA,
                listOf(
                    opprettDelberegningSumInntekt().copy(
                        grunnlagsreferanseListe = listOf("INNTEKT_MANUEL", "INNTEKT_BARNETILLEGG", "INNTEKT_KONTANTSTØTTE"),
                        innhold =
                            POJONode(
                                DelberegningSumInntekt(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    totalinntekt = BigDecimal(290000),
                                ),
                            ),
                    ),
                    opprettInntektsrapportering().copy(
                        referanse = "INNTEKT_KONTANTSTØTTE",
                        innhold =
                            POJONode(
                                InntektsrapporteringPeriode(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    beløp = BigDecimal(20000),
                                    manueltRegistrert = true,
                                    inntektsrapportering = Inntektsrapportering.KONTANTSTØTTE,
                                    valgt = true,
                                ),
                            ),
                        gjelderBarnReferanse = persongrunnlagBA.referanse,
                    ),
                    opprettInntektsrapportering().copy(
                        referanse = "INNTEKT_BARNETILLEGG",
                        innhold =
                            POJONode(
                                InntektsrapporteringPeriode(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    beløp = BigDecimal(20000),
                                    manueltRegistrert = true,
                                    inntektsrapportering = Inntektsrapportering.BARNETILLEGG,
                                    valgt = true,
                                ),
                            ),
                        gjelderBarnReferanse = persongrunnlagBA.referanse,
                    ),
                    opprettInntektsrapportering().copy(
                        referanse = "INNTEKT_MANUEL",
                        innhold =
                            POJONode(
                                InntektsrapporteringPeriode(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    beløp = BigDecimal(25000),
                                    manueltRegistrert = true,
                                    inntektsrapportering = Inntektsrapportering.LØNN_MANUELT_BEREGNET,
                                    valgt = true,
                                ),
                            ),
                    ),
                ),
            )
        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidForskudd2)) } returns
            opprettForskuddVedtakRespons(
                søknadsbarn = persongrunnlagBA2,
                listOf(
                    opprettDelberegningSumInntekt().copy(
                        grunnlagsreferanseListe = listOf("INNTEKT_MANUEL", "INNTEKT_BARNETILLEGG", "INNTEKT_KONTANTSTØTTE"),
                        innhold =
                            POJONode(
                                DelberegningSumInntekt(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    totalinntekt = BigDecimal(270000),
                                ),
                            ),
                    ),
                    opprettInntektsrapportering().copy(
                        referanse = "INNTEKT_KONTANTSTØTTE",
                        innhold =
                            POJONode(
                                InntektsrapporteringPeriode(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    beløp = BigDecimal(10000),
                                    manueltRegistrert = true,
                                    inntektsrapportering = Inntektsrapportering.KONTANTSTØTTE,
                                    valgt = true,
                                ),
                            ),
                        gjelderBarnReferanse = persongrunnlagBA2.referanse,
                    ),
                    opprettInntektsrapportering().copy(
                        referanse = "INNTEKT_BARNETILLEGG",
                        innhold =
                            POJONode(
                                InntektsrapporteringPeriode(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    beløp = BigDecimal(10000),
                                    manueltRegistrert = true,
                                    inntektsrapportering = Inntektsrapportering.BARNETILLEGG,
                                    valgt = true,
                                ),
                            ),
                        gjelderBarnReferanse = persongrunnlagBA2.referanse,
                    ),
                    opprettInntektsrapportering().copy(
                        referanse = "INNTEKT_MANUEL",
                        innhold =
                            POJONode(
                                InntektsrapporteringPeriode(
                                    periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                    beløp = BigDecimal(25000),
                                    manueltRegistrert = true,
                                    inntektsrapportering = Inntektsrapportering.LØNN_MANUELT_BEREGNET,
                                    valgt = true,
                                ),
                            ),
                    ),
                ),
            )

        every { bidragVedtakConsumer.hentVedtak(eq(vedtaksidBidrag)) } returns
            opprettVedtakDto().copy(
                grunnlagListe =
                    opprettGrunnlagslisteBidrag(
                        listOf(
                            opprettDelberegningSumInntekt().copy(
                                grunnlagsreferanseListe = listOf("INNTEKT_MANUEL", "INNTEKT_BARNETILLEGG", "INNTEKT_KONTANTSTØTTE"),
                                innhold =
                                    POJONode(
                                        DelberegningSumInntekt(
                                            periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                            totalinntekt = BigDecimal(370000),
                                        ),
                                    ),
                            ),
                            opprettInntektsrapportering().copy(
                                referanse = "INNTEKT_KONTANTSTØTTE",
                                innhold =
                                    POJONode(
                                        InntektsrapporteringPeriode(
                                            periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                            beløp = BigDecimal(10000),
                                            manueltRegistrert = true,
                                            inntektsrapportering = Inntektsrapportering.KONTANTSTØTTE,
                                            valgt = true,
                                        ),
                                    ),
                                gjelderBarnReferanse = persongrunnlagBA.referanse,
                            ),
                            opprettInntektsrapportering().copy(
                                referanse = "INNTEKT_BARNETILLEGG",
                                innhold =
                                    POJONode(
                                        InntektsrapporteringPeriode(
                                            periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                            beløp = BigDecimal(10000),
                                            manueltRegistrert = true,
                                            inntektsrapportering = Inntektsrapportering.BARNETILLEGG,
                                            valgt = true,
                                        ),
                                    ),
                                gjelderBarnReferanse = persongrunnlagBA.referanse,
                            ),
                            opprettInntektsrapportering().copy(
                                referanse = "INNTEKT_MANUEL",
                                innhold =
                                    POJONode(
                                        InntektsrapporteringPeriode(
                                            periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                                            beløp = BigDecimal(35000),
                                            manueltRegistrert = true,
                                            inntektsrapportering = Inntektsrapportering.LØNN_MANUELT_BEREGNET,
                                            valgt = true,
                                        ),
                                    ),
                            ),
                        ),
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
        resultat.shouldHaveSize(2)
        resultat.first().gjelderBarn shouldBe personIdentSøknadsbarn2
        resultat.first().bidragsmottaker shouldBe personIdentBidragsmottaker
        resultat.first().saksnummer shouldBe saksnummer
        resultat[0].stønadstype shouldBe Stønadstype.BIDRAG

        resultat[1].gjelderBarn shouldBe personIdentSøknadsbarn1
        resultat[1].bidragsmottaker shouldBe personIdentBidragsmottaker
        resultat[1].saksnummer shouldBe saksnummer
        resultat[1].stønadstype shouldBe Stønadstype.BIDRAG

        verify(exactly = 1) { bidragVedtakConsumer.hentVedtak(vedtaksidForskudd) }
        verify(exactly = 1) { bidragVedtakConsumer.hentVedtak(vedtaksidForskudd) }
        verify(exactly = 1) { bidragSakConsumer.hentSak(saksnummer) }
        verify(exactly = 1) {
            bidragBeløpshistorikkConsumer.hentHistoriskeStønader(
                withArg {
                    it.kravhaver shouldBe Personident(personIdentSøknadsbarn2)
                },
            )
        }
        verify(exactly = 1) {
            bidragBeløpshistorikkConsumer.hentHistoriskeStønader(
                withArg {
                    it.kravhaver shouldBe Personident(personIdentSøknadsbarn1)
                },
            )
        }
    }
}

private fun opprettForskuddVedtakRespons(
    søknadsbarn: GrunnlagDto = persongrunnlagBA,
    inntekter: List<GrunnlagDto> =
        listOf(
            opprettDelberegningSumInntekt().copy(
                innhold =
                    POJONode(
                        DelberegningSumInntekt(
                            periode = ÅrMånedsperiode(YearMonth.parse("2024-01"), null),
                            totalinntekt = BigDecimal(300000),
                        ),
                    ),
            ),
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
        ),
) = opprettVedtakDto().copy(
    grunnlagListe =
        listOf(
            søknadsbarn,
            testdataBidragsmottaker.tilGrunnlag(),
            opprettGrunnlagSluttberegningForskudd(),
            opprettSivilstandPeriode(),
            opprettGrunnlagSøknad(),
            opprettBostatatusperiode(søknadsbarn),
            opprettDelberegningBarnIHusstand(),
        ) + inntekter,
    stønadsendringListe =
        listOf(
            opprettStønadsendringForskudd(søknadsbarn),
        ),
)

private fun opprettGrunnlagslisteBidrag(
    inntekter: List<GrunnlagDto> =
        listOf(
            opprettGrunnlagDelberegningAndel(),
            opprettDelberegningSumInntekt(),
        ),
) = listOf(
    persongrunnlagBA,
    testdataBidragsmottaker.tilGrunnlag(),
    persongrunnlagBP,
    opprettGrunnlagSøknad(),
    opprettGrunnlagSluttberegningBidrag(),
    opprettGrunnlagDelberegningAndel(),
) + inntekter

private fun opprettLøpendeForskuddRespons(
    vedtaksid: Int = vedtaksidForskudd,
    beløp: BigDecimal = BigDecimal("5600"),
) = opprettStønadDto(
    stønadstype = Stønadstype.FORSKUDD,
    periodeListe =
        listOf(
            opprettStønadPeriodeDto(
                ÅrMånedsperiode(LocalDate.now().minusMonths(4), null),
                beløp = beløp,
                vedtakId = vedtaksid,
            ),
        ),
)
