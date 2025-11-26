package no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd

import com.fasterxml.jackson.databind.node.POJONode
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragGrunnlagConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.mapper.VedtakMapper
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import no.nav.bidrag.automatiskjobb.service.ReskontroService
import no.nav.bidrag.beregn.barnebidrag.service.VedtakService
import no.nav.bidrag.beregn.forskudd.BeregnForskuddApi
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.inntekt.InntektApi
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadPeriodeDto
import no.nav.bidrag.transport.behandling.beregning.forskudd.BeregnetForskuddResultat
import no.nav.bidrag.transport.behandling.beregning.forskudd.ResultatBeregning
import no.nav.bidrag.transport.behandling.beregning.forskudd.ResultatPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.inntekt.response.SummertMånedsinntekt
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Year
import java.time.YearMonth
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
class EvaluerRevurderForskuddServiceTest {
    @MockK(relaxed = true)
    private lateinit var vedtakService: VedtakService

    @MockK(relaxed = true)
    private lateinit var revurderForskuddRepository: RevurderForskuddRepository

    @MockK(relaxed = true)
    private lateinit var bidragBeløpshistorikkConsumer: BidragBeløpshistorikkConsumer

    @MockK(relaxed = true)
    private lateinit var inntektApi: InntektApi

    @MockK(relaxed = true)
    private lateinit var beregnForskuddApi: BeregnForskuddApi

    @MockK(relaxed = true)
    private lateinit var bidragSakConsumer: BidragSakConsumer

    @MockK(relaxed = true)
    private lateinit var bidragReskontroService: ReskontroService

    @MockK(relaxed = true)
    private lateinit var bidragVedtakConsumer: BidragVedtakConsumer

    @MockK(relaxed = true)
    private lateinit var bidragGrunnlagConsumer: BidragGrunnlagConsumer

    @MockK(relaxed = true)
    private lateinit var vedtakMapper: VedtakMapper

    @InjectMockKs
    private lateinit var evaluerRevurderForskuddService: EvaluerRevurderForskuddService

    @Test
    fun skalIkkeFortsetteUtenManueltVedtak() {
        val revurderingForskudd = mockk<RevurderingForskudd>(relaxed = true)
        every { vedtakService.finnSisteManuelleVedtak(any()) } returns null

        evaluerRevurderForskuddService.evaluerRevurderForskudd(
            revurderingForskudd,
            simuler = false,
            antallMånederForBeregning = 12,
            beregnFraMåned = YearMonth.now(),
        )

        verify(exactly = 0) { revurderForskuddRepository.save(any()) }
    }

    @Test
    fun skalIkkeFortsetteNårForskuddErNull() {
        val revurderingForskudd = mockk<RevurderingForskudd>(relaxed = true)
        every { vedtakService.finnSisteManuelleVedtak(any()) } returns mockk()
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns null

        evaluerRevurderForskuddService.evaluerRevurderForskudd(
            revurderingForskudd,
            simuler = false,
            antallMånederForBeregning = 6,
            beregnFraMåned = YearMonth.now(),
        )

        verify(exactly = 0) { revurderForskuddRepository.save(any()) }
    }

    @Test
    fun skalIkkeFortsetteNårForskuddIkkeLøpende() {
        val revurderingForskudd = mockk<RevurderingForskudd>(relaxed = true)
        val stønadDto =
            mockk<StønadDto> {
                every { periodeListe } returns emptyList()
            }
        every { vedtakService.finnSisteManuelleVedtak(any()) } returns mockk()
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns stønadDto

        evaluerRevurderForskuddService.evaluerRevurderForskudd(
            revurderingForskudd,
            simuler = false,
            antallMånederForBeregning = 12,
            beregnFraMåned = YearMonth.now(),
        )

        verify(exactly = 0) { revurderForskuddRepository.save(any()) }
    }

    @Test
    fun skalSimulereNårSimulerErSatt() {
        val barnFnr = PersonidentGenerator.genererFødselsnummer()
        val bmFnr = PersonidentGenerator.genererFødselsnummer()
        val revurderingForskudd =
            mockk<RevurderingForskudd>(relaxed = true) {
                every { barn } returns
                    mockk<Barn>(relaxed = true) {
                        every { kravhaver } returns barnFnr
                    }
            }
        val stønadPeriodeDto =
            mockk<StønadPeriodeDto>(relaxed = true) {
                every { periode.til } returns null
                every { beløp } returns BigDecimal(100)
            }
        val stønadDto =
            mockk<StønadDto>(relaxed = true) {
                every { periodeListe } returns listOf(stønadPeriodeDto)
            }
        every { vedtakService.finnSisteManuelleVedtak(any()) } returns
            mockk {
                every { vedtak.grunnlagListe } returns
                    listOf(
                        GrunnlagDto(
                            "barn",
                            Grunnlagstype.PERSON_SØKNADSBARN,
                            POJONode(Person(ident = Personident(barnFnr))),
                        ),
                        GrunnlagDto(
                            "bm",
                            Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                            POJONode(
                                Person(
                                    ident = Personident(bmFnr),
                                ),
                            ),
                        ),
                    )
            }
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns stønadDto
        every { inntektApi.transformerInntekter(any()) } returns
            mockk {
                every { summertMånedsinntektListe } returns emptyList()
            }
        every { revurderForskuddRepository.save(any()) } returns mockk()

        evaluerRevurderForskuddService.evaluerRevurderForskudd(
            revurderingForskudd,
            simuler = true,
            antallMånederForBeregning = 12,
            beregnFraMåned = YearMonth.now(),
        )

        verify { revurderingForskudd.status = Status.SIMULERT }
        verify { revurderForskuddRepository.save(revurderingForskudd) }
        verify(exactly = 0) { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(any()) }
    }

    @Test
    fun skalSetteStatusBehandletNårIngenNedsettelse() {
        val barnFnr = PersonidentGenerator.genererFødselsnummer()
        val bmFnr = PersonidentGenerator.genererFødselsnummer()
        val revurderingForskudd =
            mockk<RevurderingForskudd>(relaxed = true) {
                every { barn } returns
                    mockk<Barn>(relaxed = true) {
                        every { kravhaver } returns barnFnr
                    }
            }
        val stønadPeriodeDto =
            mockk<StønadPeriodeDto>(relaxed = true) {
                every { periode.til } returns null
                every { beløp } returns BigDecimal(100)
            }
        val stønadDto =
            mockk<StønadDto>(relaxed = true) {
                every { periodeListe } returns listOf(stønadPeriodeDto)
            }
        every { vedtakService.finnSisteManuelleVedtak(any()) } returns
            mockk {
                every { vedtak.grunnlagListe } returns
                    listOf(
                        GrunnlagDto(
                            "barn",
                            Grunnlagstype.PERSON_SØKNADSBARN,
                            POJONode(Person(ident = Personident(barnFnr))),
                        ),
                        GrunnlagDto(
                            "bm",
                            Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                            POJONode(
                                Person(
                                    ident = Personident(bmFnr),
                                ),
                            ),
                        ),
                    )
            }
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns stønadDto
        every { inntektApi.transformerInntekter(any()) } returns
            mockk {
                every { summertMånedsinntektListe } returns emptyList()
            }
        every { revurderForskuddRepository.save(any()) } returns mockk()

        evaluerRevurderForskuddService.evaluerRevurderForskudd(
            revurderingForskudd,
            simuler = false,
            antallMånederForBeregning = 12,
            beregnFraMåned = YearMonth.now(),
        )

        verify { revurderingForskudd.status = Status.BEHANDLET }
        verify { revurderForskuddRepository.save(revurderingForskudd) }
        verify(exactly = 0) { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(any()) }
    }

    @Test
    fun skalSetteStatusBehandletOgOppretteVedtaksforslagVedNedsettelse() {
        val barnFnr = PersonidentGenerator.genererFødselsnummer()
        val bmFnr = PersonidentGenerator.genererFødselsnummer()
        val revurderingForskudd =
            mockk<RevurderingForskudd>(relaxed = true) {
                every { barn } returns
                    mockk<Barn>(relaxed = true) {
                        every { kravhaver } returns barnFnr
                    }
            }
        val stønadPeriodeDto =
            mockk<StønadPeriodeDto>(relaxed = true) {
                every { periode.til } returns null
                every { beløp } returns BigDecimal(100)
            }
        val stønadDto =
            mockk<StønadDto>(relaxed = true) {
                every { periodeListe } returns listOf(stønadPeriodeDto)
            }
        every { vedtakService.finnSisteManuelleVedtak(any()) } returns
            mockk {
                every { vedtak.grunnlagListe } returns
                    listOf(
                        GrunnlagDto(
                            "barn",
                            Grunnlagstype.PERSON_SØKNADSBARN,
                            POJONode(Person(ident = Personident(barnFnr))),
                        ),
                        GrunnlagDto(
                            "bm",
                            Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                            POJONode(
                                Person(
                                    ident = Personident(bmFnr),
                                ),
                            ),
                        ),
                    )
                every { vedtaksId } returns 1
            }
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns stønadDto
        every { inntektApi.transformerInntekter(any()) } returns
            mockk {
                every { summertMånedsinntektListe } returns
                    listOf(
                        SummertMånedsinntekt(
                            YearMonth.now().minusMonths(1),
                            BigDecimal(1000),
                            emptyList(),
                        ),
                    )
            }
        every { revurderForskuddRepository.save(any()) } returns mockk()
        every { beregnForskuddApi.beregn(any()) } returns
            mockk<BeregnetForskuddResultat>(relaxed = true) {
                every { beregnetForskuddPeriodeListe } returns
                    listOf(
                        ResultatPeriode(
                            periode = ÅrMånedsperiode(Year.now().atMonth(1), null),
                            resultat =
                                ResultatBeregning(
                                    belop = BigDecimal(12),
                                    kode = Resultatkode.REDUSERT_FORSKUDD_50_PROSENT,
                                    regel = "Regel",
                                ),
                            grunnlagsreferanseListe = emptyList(),
                        ),
                    )
            }

        evaluerRevurderForskuddService.evaluerRevurderForskudd(
            revurderingForskudd,
            simuler = false,
            antallMånederForBeregning = 12,
            beregnFraMåned = YearMonth.now(),
        )

        verify { revurderingForskudd.status = Status.BEHANDLET }
        verify { revurderForskuddRepository.save(revurderingForskudd) }
        verify { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(any()) }
    }
}
