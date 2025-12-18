package no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd

import com.fasterxml.jackson.databind.node.POJONode
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragGrunnlagConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.mapper.VedtakMapper
import no.nav.bidrag.automatiskjobb.mapper.tilOpprettGrunnlagRequestDto
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.service.ReskontroService
import no.nav.bidrag.beregn.barnebidrag.service.SisteManuelleVedtak
import no.nav.bidrag.beregn.barnebidrag.service.VedtakService
import no.nav.bidrag.beregn.barnebidrag.utils.hentSisteLøpendePeriode
import no.nav.bidrag.beregn.forskudd.BeregnForskuddApi
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.vedtak.BehandlingsrefKilde
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Formål
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.felles.personidentNav
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.inntekt.InntektApi
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.beregning.forskudd.BeregnetForskuddResultat
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.bidragsmottaker
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPersonMedIdent
import no.nav.bidrag.transport.behandling.grunnlag.request.GrunnlagRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.request.HentGrunnlagRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.response.AinntektGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.HentGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.SkattegrunnlagGrunnlagDto
import no.nav.bidrag.transport.behandling.inntekt.request.Ainntektspost
import no.nav.bidrag.transport.behandling.inntekt.request.SkattegrunnlagForLigningsår
import no.nav.bidrag.transport.behandling.inntekt.request.TransformerInntekterRequest
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettBehandlingsreferanseRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettPeriodeRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettStønadsendringRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.bidrag.transport.behandling.vedtak.response.behandlingId
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private val LOGGER = KotlinLogging.logger { }

@Service
@Import(BeregnForskuddApi::class, InntektApi::class)
class EvaluerRevurderForskuddService(
    private val vedtakService: VedtakService,
    private val bidragBeløpshistorikkConsumer: BidragBeløpshistorikkConsumer,
    private val inntektApi: InntektApi,
    private val beregnForskuddApi: BeregnForskuddApi,
    private val bidragReskontroService: ReskontroService,
    private val bidragSakConsumer: BidragSakConsumer,
    private val vedtakMapper: VedtakMapper,
    private val bidragVedtakConsumer: BidragVedtakConsumer,
    private val bidragGrunnlagConsumer: BidragGrunnlagConsumer,
) {
    fun evaluerRevurderForskudd(
        revurderingForskudd: RevurderingForskudd,
        simuler: Boolean,
        antallMånederForBeregning: Long,
        beregnFraMåned: YearMonth,
    ): RevurderingForskudd? {
        val sisteManuelleVedtak = finnSisteManuelleVedtak(revurderingForskudd)
        if (sisteManuelleVedtak == null) {
            LOGGER.info {
                "Fant ingen manuelle vedtak for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer}. Beregner ikke revurdering av forskudd."
            }
            revurderingForskudd.behandlingstype = Behandlingstype.INGEN
            revurderingForskudd.status = Status.BEHANDLET
            return revurderingForskudd
        }

        val forskudd = hentForskuddForSak(revurderingForskudd.barn.saksnummer, revurderingForskudd.barn.kravhaver)
        if (forskudd == null || !erForskuddLøpende(forskudd)) {
            LOGGER.info {
                "Forskudd $forskudd er ikke løpende for revurderingForskudd $revurderingForskudd! Beregner ikke revurdering av forskudd."
            }
            revurderingForskudd.behandlingstype = Behandlingstype.INGEN
            revurderingForskudd.status = Status.BEHANDLET
            return revurderingForskudd
        }

        if (sisteManuelleVedtak.vedtak.kildeapplikasjon == "bisys") {
            LOGGER.info {
                "Siste manuelle vedtak for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer} er fra BISYS. Beregner ikke revurdering av forskudd."
            }
            revurderingForskudd.behandlingstype = Behandlingstype.INGEN
            revurderingForskudd.status = Status.BEHANDLET
            return revurderingForskudd
        }

        if (sisteManuelleVedtak.vedtak.behandlingId == null) {
            LOGGER.info {
                "Siste manuelle vedtak for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer} har ingen behandlingId. Beregner ikke revurdering av forskudd."
            }
            revurderingForskudd.behandlingstype = Behandlingstype.INGEN
            revurderingForskudd.status = Status.BEHANDLET
            return revurderingForskudd
        }

        val barnGrunnlagReferanse =
            sisteManuelleVedtak.vedtak.grunnlagListe
                .hentPersonMedIdent(
                    revurderingForskudd.barn.kravhaver,
                )!!
                .referanse

        val bmGrunnlagReferanse =
            sisteManuelleVedtak.vedtak.grunnlagListe.bidragsmottaker!!
                .referanse

        // Filterer ut grunnlag som gjelder andre barn enn det som revurderes
        val filtrertGrunnlagForBarn =
            sisteManuelleVedtak.vedtak.grunnlagListe
                .filter {
                    it.gjelderBarnReferanse == null ||
                        it.gjelderBarnReferanse == barnGrunnlagReferanse
                }.toMutableList()

        // Finner inntekter for forskuddsmottaker fra grunnlaget
        val transformerteInntekter = finnInntekterForForskuddFraGrunnlaget(forskudd)

        // Finn laveste summert månedsinntekt for måneder i perioden ganget med 12
        val lavesteMånedsinntekt =
            (antallMånederForBeregning downTo 1)
                .mapNotNull { i ->
                    val måned = YearMonth.now().minusMonths(i)
                    transformerteInntekter.summertMånedsinntektListe
                        .find { måned.equals(it.gjelderÅrMåned) }
                        ?.sumInntekt
                }.minOrNull()
                ?.multiply(BigDecimal(12))

        //  Finner summert årsintekt basert på siste 12 månedsinntekter
        val årsinntekt =
            (12L downTo 1)
                .mapNotNull { i ->
                    val måned = YearMonth.now().minusMonths(i)
                    transformerteInntekter.summertMånedsinntektListe
                        .find { måned.equals(it.gjelderÅrMåned) }
                        ?.sumInntekt ?: BigDecimal.ZERO
                }.reduce { årsinntekt, månedsinntekt -> årsinntekt.add(månedsinntekt) }

        val beregnetForskuddLavesteMånedsinntekt =
            lavesteMånedsinntekt?.let {
                beregnNyttForskudd(
                    filtrertGrunnlagForBarn,
                    beregnFraMåned,
                    barnGrunnlagReferanse,
                    bmGrunnlagReferanse,
                    it,
                )
            }
        val beregnetForskuddÅrsinntekt =
            beregnNyttForskudd(
                filtrertGrunnlagForBarn,
                beregnFraMåned,
                barnGrunnlagReferanse,
                bmGrunnlagReferanse,
                årsinntekt,
            )

        val løpendeBeløp = forskudd.periodeListe.hentSisteLøpendePeriode()!!.beløp!!

        val skalSetteNedForskuddÅrsinntekt = skalForskuddSettesNed(løpendeBeløp, beregnetForskuddÅrsinntekt)
        val skalSetteNedForskuddMånedsinntekt =
            skalForskuddSettesNed(løpendeBeløp, beregnetForskuddLavesteMånedsinntekt)

        // Simuleringsmodus er slått på og ingen endring skal gjøres
        if (simuler) {
            if (skalSetteNedForskuddÅrsinntekt || skalSetteNedForskuddMånedsinntekt) {
                LOGGER.info {
                    "Simulering: Forskudd for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer} skal settes ned etter revurdering."
                }
                revurderingForskudd.behandlingstype = Behandlingstype.FATTET_FORSLAG
            } else {
                LOGGER.info {
                    "Simulering: Forskudd for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer} skal ikke settes ned etter revurdering."
                }
                revurderingForskudd.behandlingstype = Behandlingstype.INGEN
            }
            revurderingForskudd.status = Status.SIMULERT
            revurderingForskudd.behandlingstype = Behandlingstype.FATTET_FORSLAG
            return revurderingForskudd
        }

        // Ingen endring i forskudd skal gjøres
        if (!skalSetteNedForskuddMånedsinntekt && !skalSetteNedForskuddÅrsinntekt) {
            LOGGER.info {
                "Forskudd for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer} skal ikke settes ned etter revurdering."
            }
            revurderingForskudd.status = Status.BEHANDLET
            revurderingForskudd.behandlingstype = Behandlingstype.INGEN
            return revurderingForskudd
        }

        // Gjør en sjekk mot reskontro for å se om det eksisterer A4 transaksjoner (forskudd) for de siste 3 månedene. Dette gjøres for å kunne opprette oppgaver for tilbakekreving det er utbetalt forskudd
        if (bidragReskontroService.finnesForskuddForSakPeriode(
                Saksnummer(revurderingForskudd.barn.saksnummer),
                listOf(
                    LocalDate.now().minusMonths(3),
                    LocalDate.now().minusMonths(2),
                    LocalDate.now().minusMonths(1),
                ),
            )
        ) {
            revurderingForskudd.vurdereTilbakekreving = true
        }

        val opprettVedtakRequestDto =
            opprettVedtaksforslag(
                revurderingForskudd,
                skalSetteNedForskuddÅrsinntekt,
                beregnetForskuddÅrsinntekt,
                beregnetForskuddLavesteMånedsinntekt,
            )
        val vedtakId = bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(opprettVedtakRequestDto)

        revurderingForskudd.vedtaksidBeregning = sisteManuelleVedtak.vedtaksId
        revurderingForskudd.vedtak = vedtakId
        revurderingForskudd.status = Status.BEHANDLET
        revurderingForskudd.behandlingstype = Behandlingstype.FATTET_FORSLAG

        return revurderingForskudd
    }

    private fun opprettVedtaksforslag(
        revurderingForskudd: RevurderingForskudd,
        skalSetteNedForskuddÅrsinntekt: Boolean,
        beregnetForskuddÅrsinntekt: BeregnetForskuddResultat,
        beregnetForskuddLavesteMånedsinntekt: BeregnetForskuddResultat?,
    ): OpprettVedtakRequestDto {
        val sak = bidragSakConsumer.hentSak(revurderingForskudd.barn.saksnummer)
        val sakrolleBarn = vedtakMapper.hentBarn(sak, revurderingForskudd.barn.kravhaver)
        val mottaker = vedtakMapper.reellMottakerEllerBidragsmottaker(sakrolleBarn, sak.roller)!!
        val beregnetForskuddResultat =
            if (skalSetteNedForskuddÅrsinntekt) {
                beregnetForskuddÅrsinntekt
            } else {
                beregnetForskuddLavesteMånedsinntekt!!
            }

        val opprettVedtakRequestDto =
            OpprettVedtakRequestDto(
                type = Vedtakstype.REVURDERING,
                vedtakstidspunkt = null,
                unikReferanse = revurderingForskudd.unikReferanse,
                enhetsnummer = Enhetsnummer("9999"),
                stønadsendringListe =
                    listOf(
                        OpprettStønadsendringRequestDto(
                            type = Stønadstype.FORSKUDD,
                            sak = Saksnummer(revurderingForskudd.barn.saksnummer),
                            kravhaver = Personident(revurderingForskudd.barn.kravhaver),
                            skyldner = Personident(IdentUtils.NAV_TSS_IDENT),
                            mottaker = mottaker,
                            beslutning = Beslutningstype.ENDRING,
                            grunnlagReferanseListe = emptyList(),
                            innkreving = Innkrevingstype.MED_INNKREVING,
                            sisteVedtaksid =
                                vedtakService.finnSisteVedtaksid(
                                    revurderingForskudd.barn.tilStønadsid(
                                        Stønadstype.FORSKUDD,
                                    ),
                                ),
                            førsteIndeksreguleringsår = YearMonth.now().plusYears(1).year,
                            periodeListe =
                                beregnetForskuddResultat.beregnetForskuddPeriodeListe.map {
                                    OpprettPeriodeRequestDto(
                                        periode = it.periode,
                                        beløp = it.resultat.belop,
                                        valutakode = "NOK",
                                        resultatkode = it.resultat.kode.name,
                                        grunnlagReferanseListe = it.grunnlagsreferanseListe,
                                    )
                                },
                        ),
                    ),
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe =
                    listOf(
                        OpprettBehandlingsreferanseRequestDto(
                            kilde = BehandlingsrefKilde.REVURDERING_FORSKUDD,
                            referanse = revurderingForskudd.batchId,
                        ),
                    ),
                grunnlagListe = beregnetForskuddResultat.grunnlagListe.map { it.tilOpprettGrunnlagRequestDto() },
                kilde = Vedtakskilde.AUTOMATISK,
            )
        return opprettVedtakRequestDto
    }

    private fun finnSisteManuelleVedtak(revurderingForskudd: RevurderingForskudd): SisteManuelleVedtak? =
        vedtakService.finnSisteManuelleVedtak(
            Stønadsid(
                Stønadstype.FORSKUDD,
                Personident(revurderingForskudd.barn.kravhaver),
                Personident("NAV"),
                Saksnummer(revurderingForskudd.barn.saksnummer),
            ),
        )

    private fun hentForskuddForSak(
        saksnummer: String,
        søknadsbarnIdent: String,
    ): StønadDto? =
        bidragBeløpshistorikkConsumer.hentHistoriskeStønader(
            HentStønadHistoriskRequest(
                type = Stønadstype.FORSKUDD,
                sak = Saksnummer(saksnummer),
                skyldner = personidentNav,
                kravhaver = Personident(søknadsbarnIdent),
                gyldigTidspunkt = LocalDateTime.now(),
            ),
        )

    private fun erForskuddLøpende(stønadDto: StønadDto): Boolean =
        if (stønadDto.periodeListe.hentSisteLøpendePeriode() != null) {
            true
        } else {
            false.also {
                LOGGER.info {
                    "Forskudd i sak ${stønadDto.sak.verdi} og barn ${stønadDto.kravhaver.verdi} har opphørt før dagens dato. Det finnes ingen løpende forskudd"
                }
            }
        }

    private fun finnInntekterForForskuddFraGrunnlaget(forskudd: StønadDto): TransformerInntekterResponse {
        val grunnlag = hentInntektsGrunnlagForForskudd(forskudd)
        val transformerInntekterRequest = transformerInntekter(grunnlag)
        val transformerInntekterResponse = inntektApi.transformerInntekter(transformerInntekterRequest)
        return transformerInntekterResponse
    }

    private fun transformerInntekter(grunnlag: HentGrunnlagDto): TransformerInntekterRequest =
        TransformerInntekterRequest(
            ainntektHentetDato = LocalDate.now(),
            ainntektsposter = grunnlag.ainntektListe.tilAInntektsPost(),
            skattegrunnlagsliste = grunnlag.skattegrunnlagListe.tilSkattegrunnlagForLigningsår(),
        )

    private fun List<AinntektGrunnlagDto>.tilAInntektsPost(): List<Ainntektspost> =
        this.flatMap { it.ainntektspostListe }.map {
            Ainntektspost(
                utbetalingsperiode = it.utbetalingsperiode,
                opptjeningsperiodeFra = it.opptjeningsperiodeFra,
                opptjeningsperiodeTil = it.opptjeningsperiodeTil,
                etterbetalingsperiodeFra = it.etterbetalingsperiodeFra,
                etterbetalingsperiodeTil = it.etterbetalingsperiodeTil,
                beskrivelse = it.beskrivelse,
                beløp = it.beløp,
            )
        }

    fun List<SkattegrunnlagGrunnlagDto>.tilSkattegrunnlagForLigningsår(): List<SkattegrunnlagForLigningsår> =
        this.map {
            SkattegrunnlagForLigningsår(
                ligningsår = it.periodeFra.year,
                skattegrunnlagsposter = it.skattegrunnlagspostListe,
            )
        }

    private fun hentInntektsGrunnlagForForskudd(forskudd: StønadDto): HentGrunnlagDto =
        bidragGrunnlagConsumer.hentGrunnlag(
            HentGrunnlagRequestDto(
                Formål.FORSKUDD,
                listOf(
                    GrunnlagRequestDto(
                        GrunnlagRequestType.AINNTEKT,
                        forskudd.mottaker.verdi,
                        LocalDate.now().minusYears(1),
                        LocalDate.now(),
                    ),
                    GrunnlagRequestDto(
                        GrunnlagRequestType.SKATTEGRUNNLAG,
                        forskudd.mottaker.verdi,
                        LocalDate.now().minusYears(3),
                        LocalDate.now(),
                    ),
                ),
            ),
        )

    private fun beregnNyttForskudd(
        filtrertGrunnlagForBarn: MutableList<GrunnlagDto>,
        beregnFraMåned: YearMonth,
        barnGrunnlagReferanse: String,
        bmGrunnlagReferanse: String,
        inntekt: BigDecimal,
    ): BeregnetForskuddResultat {
        filtrertGrunnlagForBarn.removeIf { it.type == Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE }
        filtrertGrunnlagForBarn.add(
            GrunnlagDto(
                type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                innhold =
                    POJONode(
                        InntektsrapporteringPeriode(
                            periode = ÅrMånedsperiode(beregnFraMåned, null),
                            manueltRegistrert = true,
                            inntektsrapportering = Inntektsrapportering.AINNTEKT_BEREGNET_12MND,
                            beløp = inntekt,
                            gjelderBarn = barnGrunnlagReferanse,
                            valgt = true,
                        ),
                    ),
                referanse = "${Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE}_$bmGrunnlagReferanse",
                gjelderReferanse = bmGrunnlagReferanse,
                gjelderBarnReferanse = barnGrunnlagReferanse,
            ),
        )

        return beregnForskuddApi.beregn(
            BeregnGrunnlag(
                periode =
                    ÅrMånedsperiode(
                        YearMonth.now(),
                        YearMonth.now().plusMonths(1),
                    ),
                stønadstype = Stønadstype.FORSKUDD,
                søknadsbarnReferanse = barnGrunnlagReferanse,
                grunnlagListe = filtrertGrunnlagForBarn,
            ),
        )
    }

    private fun skalForskuddSettesNed(
        løpendeBeløp: BigDecimal,
        beregnetForskuddResultat: BeregnetForskuddResultat?,
    ): Boolean {
        if (beregnetForskuddResultat == null || beregnetForskuddResultat.beregnetForskuddPeriodeListe.isEmpty()) {
            return false
        }
        return løpendeBeløp >
            beregnetForskuddResultat.beregnetForskuddPeriodeListe
                .last()
                .resultat.belop
    }
}
