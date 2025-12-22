package no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd

import com.fasterxml.jackson.databind.node.POJONode
import io.kotest.matchers.shouldBe
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
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.service.ReskontroService
import no.nav.bidrag.beregn.barnebidrag.service.external.VedtakService
import no.nav.bidrag.beregn.forskudd.BeregnForskuddApi
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.vedtak.BehandlingsrefKilde
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
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
        val revurderingForskudd =
            RevurderingForskudd(
                forMåned = YearMonth.now().toString(),
                batchId = "123",
                barn = mockk(relaxed = true),
                status = Status.UBEHANDLET,
            )
        every { vedtakService.finnSisteManuelleVedtak(any()) } returns null

        val revurderingForskuddRetur =
            evaluerRevurderForskuddService.evaluerRevurderForskudd(
                revurderingForskudd,
                simuler = false,
                antallMånederForBeregning = 12,
                beregnFraMåned = YearMonth.now(),
            )

        revurderingForskuddRetur?.behandlingstype shouldBe Behandlingstype.INGEN
    }

    @Test
    fun skalIkkeFortsetteNårForskuddErNull() {
        val revurderingForskudd =
            RevurderingForskudd(
                forMåned = YearMonth.now().toString(),
                batchId = "123",
                barn = mockk(relaxed = true),
                status = Status.UBEHANDLET,
            )
        every { vedtakService.finnSisteManuelleVedtak(any()) } returns mockk()
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns null

        val revurderingForskuddRetur =
            evaluerRevurderForskuddService.evaluerRevurderForskudd(
                revurderingForskudd,
                simuler = false,
                antallMånederForBeregning = 6,
                beregnFraMåned = YearMonth.now(),
            )

        revurderingForskuddRetur?.behandlingstype shouldBe Behandlingstype.INGEN
    }

    @Test
    fun skalIkkeFortsetteNårForskuddIkkeLøpende() {
        val revurderingForskudd =
            RevurderingForskudd(
                forMåned = YearMonth.now().toString(),
                batchId = "123",
                barn = mockk(relaxed = true),
                status = Status.UBEHANDLET,
            )
        val stønadDto =
            mockk<StønadDto> {
                every { periodeListe } returns emptyList()
            }
        every { vedtakService.finnSisteManuelleVedtak(any()) } returns mockk()
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns stønadDto

        val revurderingForskuddRetur =
            evaluerRevurderForskuddService.evaluerRevurderForskudd(
                revurderingForskudd,
                simuler = false,
                antallMånederForBeregning = 12,
                beregnFraMåned = YearMonth.now(),
            )

        revurderingForskuddRetur?.behandlingstype shouldBe Behandlingstype.INGEN
    }

    @Test
    fun skalSimulereNårSimulerErSatt() {
        val barnFnr = genererFødselsnummer()
        val bmFnr = genererFødselsnummer()
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
                        GrunnlagDto(
                            "bostatus",
                            Grunnlagstype.BOSTATUS_PERIODE,
                            POJONode(emptyMap<String, Any>()),
                        ),
                        GrunnlagDto(
                            "inntekt_periode",
                            Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                            POJONode(emptyMap<String, Any>()),
                        ),
                        GrunnlagDto(
                            "husstandsmedlem",
                            Grunnlagstype.PERSON_HUSSTANDSMEDLEM,
                            POJONode(emptyMap<String, Any>()),
                        ),
                        GrunnlagDto(
                            "sivilstand_periode",
                            Grunnlagstype.SIVILSTAND_PERIODE,
                            POJONode(emptyMap<String, Any>()),
                        ),
                        GrunnlagDto(
                            "innhentet_husstandsmedlem",
                            Grunnlagstype.INNHENTET_HUSSTANDSMEDLEM,
                            POJONode(emptyMap<String, Any>()),
                        ),
                        GrunnlagDto(
                            "innhentet_sivilstand",
                            Grunnlagstype.INNHENTET_SIVILSTAND,
                            POJONode(emptyMap<String, Any>()),
                        ),
                    )
                every { vedtak.behandlingsreferanseListe } returns
                    listOf(
                        mockk {
                            every { kilde } returns BehandlingsrefKilde.BEHANDLING_ID
                            every { referanse } returns "123"
                        },
                    )
                every { vedtak.kildeapplikasjon } returns "kilde"
            }
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns stønadDto
        every { inntektApi.transformerInntekter(any()) } returns
            mockk {
                every { summertMånedsinntektListe } returns emptyList()
            }

        evaluerRevurderForskuddService.evaluerRevurderForskudd(
            revurderingForskudd,
            simuler = true,
            antallMånederForBeregning = 12,
            beregnFraMåned = YearMonth.now(),
        )

        verify { revurderingForskudd.status = Status.SIMULERT }
        verify(exactly = 0) { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(any()) }
    }

    @Test
    fun skalSetteStatusBehandletNårIngenNedsettelse() {
        val barnFnr = genererFødselsnummer()
        val bmFnr = genererFødselsnummer()
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
                        GrunnlagDto(
                            "bostatus",
                            Grunnlagstype.BOSTATUS_PERIODE,
                            POJONode(emptyMap<String, Any>()),
                        ),
                        GrunnlagDto(
                            "inntekt_periode",
                            Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                            POJONode(emptyMap<String, Any>()),
                        ),
                        GrunnlagDto(
                            "husstandsmedlem",
                            Grunnlagstype.PERSON_HUSSTANDSMEDLEM,
                            POJONode(emptyMap<String, Any>()),
                        ),
                        GrunnlagDto(
                            "sivilstand_periode",
                            Grunnlagstype.SIVILSTAND_PERIODE,
                            POJONode(emptyMap<String, Any>()),
                        ),
                        GrunnlagDto(
                            "innhentet_husstandsmedlem",
                            Grunnlagstype.INNHENTET_HUSSTANDSMEDLEM,
                            POJONode(emptyMap<String, Any>()),
                        ),
                        GrunnlagDto(
                            "innhentet_sivilstand",
                            Grunnlagstype.INNHENTET_SIVILSTAND,
                            POJONode(emptyMap<String, Any>()),
                        ),
                    )
                every { vedtak.behandlingsreferanseListe } returns
                    listOf(
                        mockk {
                            every { kilde } returns BehandlingsrefKilde.BEHANDLING_ID
                            every { referanse } returns "123"
                        },
                    )
                every { vedtak.kildeapplikasjon } returns "kilde"
            }
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns stønadDto
        every { inntektApi.transformerInntekter(any()) } returns
            mockk {
                every { summertMånedsinntektListe } returns emptyList()
            }
        evaluerRevurderForskuddService.evaluerRevurderForskudd(
            revurderingForskudd,
            simuler = false,
            antallMånederForBeregning = 12,
            beregnFraMåned = YearMonth.now(),
        )

        verify { revurderingForskudd.status = Status.BEHANDLET }
        verify { revurderingForskudd.behandlingstype = Behandlingstype.INGEN }
        verify(exactly = 0) { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(any()) }
    }

    @Test
    fun skalSetteStatusBehandletOgOppretteVedtaksforslagVedNedsettelse() {
        val barnFnr = genererFødselsnummer()
        val bmFnr = genererFødselsnummer()
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
                        GrunnlagDto(
                            "bostatus",
                            Grunnlagstype.BOSTATUS_PERIODE,
                            POJONode(mockk()),
                        ),
                        GrunnlagDto(
                            "inntekt_periode",
                            Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                            POJONode(mockk()),
                        ),
                        GrunnlagDto(
                            "husstandsmedlem",
                            Grunnlagstype.PERSON_HUSSTANDSMEDLEM,
                            POJONode(mockk()),
                        ),
                        GrunnlagDto(
                            "sivilstand_periode",
                            Grunnlagstype.SIVILSTAND_PERIODE,
                            POJONode(mockk()),
                        ),
                        GrunnlagDto(
                            "innhentet_husstandsmedlem",
                            Grunnlagstype.INNHENTET_HUSSTANDSMEDLEM,
                            POJONode(mockk()),
                        ),
                        GrunnlagDto(
                            "innhentet_sivilstand",
                            Grunnlagstype.INNHENTET_SIVILSTAND,
                            POJONode(mockk()),
                        ),
                    )
                every { vedtaksId } returns 1
                every { vedtak.behandlingsreferanseListe } returns
                    listOf(
                        mockk {
                            every { kilde } returns BehandlingsrefKilde.BEHANDLING_ID
                            every { referanse } returns "123"
                        },
                    )
                every { vedtak.kildeapplikasjon } returns "KkildeILDE"
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
        verify { revurderingForskudd.behandlingstype = Behandlingstype.FATTET_FORSLAG }
        verify { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(any()) }
    }
}
