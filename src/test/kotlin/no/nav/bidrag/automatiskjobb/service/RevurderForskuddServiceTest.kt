package no.nav.bidrag.automatiskjobb.service

import com.fasterxml.jackson.databind.node.POJONode
import io.getunleash.FakeUnleash
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkConstructor
import no.nav.bidrag.automatiskjobb.consumer.BidragStønadConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.testdata.opprettBostatatusperiode
import no.nav.bidrag.automatiskjobb.testdata.opprettDelberegningBarnIHusstand
import no.nav.bidrag.automatiskjobb.testdata.opprettDelberegningSumInntekt
import no.nav.bidrag.automatiskjobb.testdata.opprettGrunnlagDelberegningAndel
import no.nav.bidrag.automatiskjobb.testdata.opprettGrunnlagSluttberegningBidrag
import no.nav.bidrag.automatiskjobb.testdata.opprettGrunnlagSluttberegningForskudd
import no.nav.bidrag.automatiskjobb.testdata.opprettGrunnlagSøknad
import no.nav.bidrag.automatiskjobb.testdata.opprettInntektsrapportering
import no.nav.bidrag.automatiskjobb.testdata.opprettSivilstandPeriode
import no.nav.bidrag.automatiskjobb.testdata.opprettStønadDto
import no.nav.bidrag.automatiskjobb.testdata.opprettStønadPeriodeDto
import no.nav.bidrag.automatiskjobb.testdata.opprettStønadsendringBidrag
import no.nav.bidrag.automatiskjobb.testdata.opprettStønadsendringForskudd
import no.nav.bidrag.automatiskjobb.testdata.opprettVedtakDto
import no.nav.bidrag.automatiskjobb.testdata.opprettVedtakhendelse
import no.nav.bidrag.automatiskjobb.testdata.persongrunnlagBA
import no.nav.bidrag.automatiskjobb.testdata.persongrunnlagBM
import no.nav.bidrag.automatiskjobb.testdata.persongrunnlagBP
import no.nav.bidrag.beregn.forskudd.BeregnForskuddApi
import no.nav.bidrag.commons.web.mock.stubSjablonProvider
import no.nav.bidrag.commons.web.mock.stubSjablonService
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSumInntekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

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
        stubSjablonService()
        stubSjablonProvider()
        service = RevurderForskuddService(bidragStønadConsumer, bidragVedtakConsumer, BeregnForskuddApi())
    }

    @Test
    fun `skal sjekke om forskudd er redusert`() {
        every { bidragStønadConsumer.hentHistoriskeStønader(any()) } returns
            opprettStønadDto(
                stønadstype = Stønadstype.FORSKUDD,
                periodeListe =
                    listOf(
                        opprettStønadPeriodeDto(
                            ÅrMånedsperiode(LocalDate.now().minusMonths(4), null),
                            beløp = BigDecimal("5600"),
                            vedtakId = 1,
                        ),
                    ),
            )
        every { bidragVedtakConsumer.hentVedtak(eq(1)) } returns
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
        every { bidragVedtakConsumer.hentVedtak(eq(2)) } returns
            opprettVedtakDto().copy(
                grunnlagListe =
                    listOf(
                        persongrunnlagBA,
                        persongrunnlagBM,
                        persongrunnlagBP,
                        opprettGrunnlagSøknad(),
                        opprettGrunnlagSluttberegningBidrag(),
                        opprettInntektsrapportering(),
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
        service.erForskuddRedusert(opprettVedtakhendelse(2))
    }
}
