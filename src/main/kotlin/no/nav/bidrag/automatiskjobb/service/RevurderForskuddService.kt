package no.nav.bidrag.automatiskjobb.service

import com.fasterxml.jackson.databind.node.POJONode
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragBehandlingConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragGrunnlagConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.mapper.GrunnlagMapper
import no.nav.bidrag.automatiskjobb.mapper.VedtakMapper
import no.nav.bidrag.automatiskjobb.mapper.erBidrag
import no.nav.bidrag.automatiskjobb.mapper.tilOpprettGrunnlagRequestDto
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Forsendelsestype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderingForskuddRepository
import no.nav.bidrag.automatiskjobb.service.model.AdresseEndretResultat
import no.nav.bidrag.automatiskjobb.service.model.ForskuddRedusertResultat
import no.nav.bidrag.automatiskjobb.service.model.StønadEngangsbeløpId
import no.nav.bidrag.automatiskjobb.utils.enesteResultatkode
import no.nav.bidrag.automatiskjobb.utils.erDirekteAvslag
import no.nav.bidrag.automatiskjobb.utils.erHusstandsmedlem
import no.nav.bidrag.automatiskjobb.utils.tilResultatkode
import no.nav.bidrag.beregn.barnebidrag.service.SisteManuelleVedtak
import no.nav.bidrag.beregn.barnebidrag.service.VedtakService
import no.nav.bidrag.beregn.forskudd.BeregnForskuddApi
import no.nav.bidrag.beregn.vedtak.Vedtaksfiltrering
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.domene.enums.beregning.Resultatkode.Companion.erDirekteAvslag
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
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
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadPeriodeDto
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.beregning.forskudd.BeregnetForskuddResultat
import no.nav.bidrag.transport.behandling.beregning.forskudd.ResultatPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.bidragsmottaker
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPersonMedIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.personIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.søknadsbarn
import no.nav.bidrag.transport.behandling.grunnlag.request.GrunnlagRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.request.HentGrunnlagRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.response.AinntektGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.HentGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.SkattegrunnlagGrunnlagDto
import no.nav.bidrag.transport.behandling.inntekt.request.Ainntektspost
import no.nav.bidrag.transport.behandling.inntekt.request.SkattegrunnlagForLigningsår
import no.nav.bidrag.transport.behandling.inntekt.request.TransformerInntekterRequest
import no.nav.bidrag.transport.behandling.inntekt.response.TransformerInntekterResponse
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.behandling.vedtak.request.HentVedtakForStønadRequest
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettPeriodeRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettStønadsendringRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.behandling.vedtak.response.erDelvedtak
import no.nav.bidrag.transport.behandling.vedtak.response.erOrkestrertVedtak
import no.nav.bidrag.transport.behandling.vedtak.response.referertVedtaksid
import no.nav.bidrag.transport.behandling.vedtak.saksnummer
import no.nav.bidrag.transport.felles.ifTrue
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private fun VedtakDto.erIndeksreguleringEllerAldersjustering() =
    listOf(Vedtakstype.ALDERSJUSTERING, Vedtakstype.INDEKSREGULERING).contains(type)

private val LOGGER = KotlinLogging.logger { }

@Service
@Import(BeregnForskuddApi::class, Vedtaksfiltrering::class, InntektApi::class)
class RevurderForskuddService(
    private val bidragBeløpshistorikkConsumer: BidragBeløpshistorikkConsumer,
    private val bidragVedtakConsumer: BidragVedtakConsumer,
    private val bidragSakConsumer: BidragSakConsumer,
    private val bidragPersonConsumer: BidragPersonConsumer,
    private val beregning: BeregnForskuddApi,
    private val vedtaksFilter: Vedtaksfiltrering,
    private val vedtakService: VedtakService,
    private val bidragBehandlingConsumer: BidragBehandlingConsumer,
    private val bidragGrunnlagConsumer: BidragGrunnlagConsumer,
    private val vedtakServiceBeregn: VedtakService,
    private val vedtakMapper: VedtakMapper,
    private val revurderingForskuddRepository: RevurderingForskuddRepository,
    private val inntektApi: InntektApi,
    private val forsendelseBestillingService: ForsendelseBestillingService,
    private val oppgaveService: OppgaveService,
    private val bidragReskontroService: ReskontroService,
) {
    fun opprettRevurdereForskudd(
        barn: Barn,
        batchId: String,
        cutoffTidspunktForManueltVedtak: LocalDateTime,
    ): RevurderingForskudd? {
        if (harÅpentForskuddssak(barn)) {
            LOGGER.info { "Barn ${barn.kravhaver} har åpent forskuddssak. Oppretter ikke revurdering av forskudd." }
            return null
        }
        val sisteManuelleVedtakTidspunkt = hentSisteManuelleVedtakTidspunkt(barn)
        if (sisteManuelleVedtakTidspunkt != null && sisteManuelleVedtakTidspunkt.isAfter(cutoffTidspunktForManueltVedtak)) {
            LOGGER.info {
                "Barn ${barn.kravhaver} har manuelt vedtak opprettet $sisteManuelleVedtakTidspunkt etter cutoff tidspunkt $cutoffTidspunktForManueltVedtak. Oppretter ikke revurdering av forskudd."
            }
            return null
        }
        return RevurderingForskudd(
            forMåned = YearMonth.now().toString(),
            batchId = batchId,
            barn = barn,
            status = Status.UBEHANDLET,
        ).also {
            LOGGER.info {
                "Opprettet revurdering forskudd for barn med id ${barn.id}. $it"
            }
        }
    }

    fun beregnRevurderForskudd(
        revurderingForskudd: RevurderingForskudd,
        simuler: Boolean,
        antallMånederForBeregning: Long,
        beregnFraMåned: YearMonth,
    ) {
        val sisteManuelleVedtak = finnSisteManuelleVedtak(revurderingForskudd)
        if (sisteManuelleVedtak == null) {
            LOGGER.info {
                "Fant ingen manuelle vedtak for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer}. Beregner ikke revurdering av forskudd."
            }
            return
        }

        val forskudd = hentForskuddForSak(revurderingForskudd.barn.saksnummer, revurderingForskudd.barn.kravhaver)
        if (forskudd == null || !erForskuddLøpende(forskudd)) {
            LOGGER.info {
                "Forskudd $forskudd er ikke løpende for revurderingForskudd $revurderingForskudd! Beregner ikke revurdering av forskudd."
            }
            return
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

        if (simuler) {
            if (skalSetteNedForskuddÅrsinntekt || skalSetteNedForskuddMånedsinntekt) {
                LOGGER.info {
                    "Simulering: Forskudd for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer} skal settes ned etter revurdering."
                }
            } else {
                LOGGER.info {
                    "Simulering: Forskudd for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer} skal ikke settes ned etter revurdering."
                }
            }
            revurderingForskudd.status = Status.SIMULERT
            revurderingForskuddRepository.save(revurderingForskudd)
            return
        }

        if (!skalSetteNedForskuddMånedsinntekt && !skalSetteNedForskuddÅrsinntekt) {
            LOGGER.info {
                "Forskudd for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer} skal ikke settes ned etter revurdering."
            }
            revurderingForskudd.status = Status.BEHANDLET
            revurderingForskuddRepository.save(revurderingForskudd)
            return
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

        val sak = bidragSakConsumer.hentSak(revurderingForskudd.barn.saksnummer)
        val barn = revurderingForskudd.barn.kravhaver
        val sakrolleBarn = vedtakMapper.hentBarn(sak, barn)
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
                            // TODO(Skal det opprettes en egen behandlingsreferanse for revurdering av forskudd her?)
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
                behandlingsreferanseListe = emptyList(),
                grunnlagListe = beregnetForskuddResultat.grunnlagListe.map { it.tilOpprettGrunnlagRequestDto() },
                kilde = Vedtakskilde.AUTOMATISK,
            )
        val vedtakId = bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(opprettVedtakRequestDto)

        revurderingForskudd.vedtaksidBeregning = sisteManuelleVedtak.vedtaksId
        revurderingForskudd.vedtak = vedtakId
        revurderingForskudd.status = Status.BEHANDLET

        revurderingForskuddRepository.save(revurderingForskudd)
    }

    private fun skalForskuddSettesNed(
        løpendeBeløp: BigDecimal,
        beregnetForskuddResultat: BeregnetForskuddResultat?,
    ): Boolean {
        if (beregnetForskuddResultat == null) {
            return false
        }
        return løpendeBeløp >
            beregnetForskuddResultat.beregnetForskuddPeriodeListe
                .last()
                .resultat.belop
    }

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

        return beregning.beregn(
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

    fun fattVedtakOmRevurderingForskudd(
        revurderingForskudd: RevurderingForskudd,
        simuler: Boolean,
    ) {
        if (simuler) {
            LOGGER.info {
                "Simulering: Fatter vedtak om revurdering av forskudd for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer}."
            }
        } else {
            LOGGER.info {
                "Fatter vedtak om revurdering av forskudd for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer}."
            }
            try {
                bidragVedtakConsumer.fatteVedtaksforslag(
                    revurderingForskudd.vedtak ?: error("Aldersjustering ${revurderingForskudd.id} mangler vedtak!"),
                )
                revurderingForskudd.status = Status.FATTET
                revurderingForskudd.fattetTidspunkt = Timestamp(System.currentTimeMillis())
            } catch (e: Exception) {
                LOGGER.error(e) {
                    "Feil ved fatting av vedtak om revurdering av forskudd for barn ${revurderingForskudd.barn.kravhaver} i sak ${revurderingForskudd.barn.saksnummer}."
                }
                revurderingForskudd.status = Status.FATTE_VEDTAK_FEILET
                revurderingForskuddRepository.save(revurderingForskudd)
                throw e
            }

            if (revurderingForskudd.status == Status.FATTET) {
                val forsendelseBestillinger =
                    forsendelseBestillingService.opprettBestilling(
                        revurderingForskudd,
                        Forsendelsestype.REVURDERING_FORSKUDD,
                    )
                revurderingForskudd.forsendelseBestilling.addAll(forsendelseBestillinger)
            }
            revurderingForskuddRepository.save(revurderingForskudd)
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

    private fun finnSisteManuelleVedtak(revurderingForskudd: RevurderingForskudd): SisteManuelleVedtak? =
        vedtakServiceBeregn.finnSisteManuelleVedtak(
            Stønadsid(
                Stønadstype.FORSKUDD,
                Personident(revurderingForskudd.barn.kravhaver),
                Personident(IdentUtils.NAV_TSS_IDENT),
                Saksnummer(revurderingForskudd.barn.saksnummer),
            ),
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

    private fun hentSisteManuelleVedtakTidspunkt(barn: Barn): LocalDateTime? =
        vedtakService
            .finnSisteManuelleVedtak(
                Stønadsid(
                    Stønadstype.FORSKUDD,
                    Personident(barn.kravhaver),
                    Personident(barn.skyldner ?: ""),
                    Saksnummer(barn.saksnummer),
                ),
            )?.vedtak
            ?.opprettetTidspunkt

    private fun harÅpentForskuddssak(barn: Barn): Boolean {
        val hentÅpneBehandlingerRespons = bidragBehandlingConsumer.hentÅpneBehandlingerForBarn(barn.kravhaver)
        return hentÅpneBehandlingerRespons.åpneBehandling.any { åpneBehandling -> åpneBehandling.stønadstype == Stønadstype.FORSKUDD }
    }

    fun skalBMFortsattMottaForskuddForSøknadsbarnEtterAdresseendring(aktørId: String): List<AdresseEndretResultat> {
        val person = bidragPersonConsumer.hentPerson(Personident(aktørId))
        val barnIdent = person.ident
        val saker = bidragSakConsumer.hentSakerForPerson(barnIdent)
        return saker.mapNotNull { sak ->
            val personRolle = sak.roller.find { it.fødselsnummer == barnIdent } ?: return@mapNotNull null
            LOGGER.info {
                "Sjekker om person ${barnIdent.verdi} er barn i saken ${sak.saksnummer}, mottar forskudd og fortsatt bor hos BM etter adresseendring"
            }
            if (personRolle.type != Rolletype.BARN) {
                LOGGER.info {
                    "Person ${barnIdent.verdi} har rolle ${personRolle.type} i sak ${sak.saksnummer}. Behandler bare når barnets adresse endres. Avslutter behandling"
                }
                return@mapNotNull null
            }
            val bidragsmottaker =
                sak.roller.find { it.type == Rolletype.BIDRAGSMOTTAKER } ?: run {
                    LOGGER.info {
                        "Sak ${sak.saksnummer} har ingen bidragsmottaker. Avslutter behandling"
                    }
                    return@mapNotNull null
                }

            LOGGER.info {
                "Sjekker om barnet ${barnIdent.verdi} i sak ${sak.saksnummer} mottar forskudd og fortsatt bor hos BM etter adresseendring"
            }
            val løpendeForskudd =
                hentLøpendeForskudd(sak.saksnummer.verdi, barnIdent.verdi) ?: run {
                    LOGGER.info {
                        "Fant ingen løpende forskudd i sak ${sak.saksnummer} for barn ${barnIdent.verdi}. Avslutter behandling"
                    }
                    return@mapNotNull null
                }

            val husstandsmedlemmerBM =
                bidragPersonConsumer.hentPersonHusstandsmedlemmer(bidragsmottaker.fødselsnummer!!)
            if (husstandsmedlemmerBM.erHusstandsmedlem(barnIdent)) {
                LOGGER.info {
                    "Barn ${barnIdent.verdi} er husstandsmedlem til bidragsmottaker ${bidragsmottaker.fødselsnummer!!.verdi}. Ingen endringer kreves."
                }
                return@mapNotNull null
            }

            LOGGER.info {
                "Bidragsmottaker ${bidragsmottaker.fødselsnummer?.verdi} mottar forskudd for barn ${barnIdent.verdi} " +
                    "i sak ${sak.saksnummer} med beløp ${løpendeForskudd.beløp} ${løpendeForskudd.valutakode}. " +
                    "Barnet bor ikke lenger hos bidragsmottaker og skal derfor ikke motta forskudd lenger"
            }
            AdresseEndretResultat(
                saksnummer = sak.saksnummer.verdi,
                bidragsmottaker = bidragsmottaker.fødselsnummer!!.verdi,
                gjelderBarn = barnIdent.verdi,
                enhet = sak.eierfogd.verdi,
            )
        }
    }

    fun erForskuddRedusert(vedtakHendelse: VedtakHendelse): List<ForskuddRedusertResultat> {
        LOGGER.info {
            "Sjekker om forskuddet er redusert etter fattet vedtak ${vedtakHendelse.id} i sak ${vedtakHendelse.saksnummer}"
        }
        val vedtak = hentVedtak(vedtakHendelse.id) ?: return listOf()
        return erForskuddRedusertEtterFattetBidrag(
            SisteManuelleVedtak(
                vedtakHendelse.id,
                vedtak,
            ),
        ) + erForskuddRedusertEtterSærbidrag(SisteManuelleVedtak(vedtakHendelse.id, vedtak))
    }

    private fun erForskuddRedusertEtterSærbidrag(vedtakInfo: SisteManuelleVedtak): List<ForskuddRedusertResultat> {
        val (vedtaksid, vedtak) = vedtakInfo
        return vedtak.engangsbeløpListe
            .filter { it.type == Engangsbeløptype.SÆRBIDRAG }
            .filter {
                if (it.resultatkode.tilResultatkode()?.erDirekteAvslag() == true) {
                    LOGGER.info {
                        "Særbidrag vedtaket $vedtaksid er direkte avslag med resultat ${it.resultatkode} og har derfor ingen inntekter."
                    }
                    false
                } else {
                    true
                }
            }.groupBy { it.sak }
            .flatMap { (sak, stønader) ->
                val stønad = stønader.first()
                val sak = bidragSakConsumer.hentSak(sak.verdi)
                sak.roller.filter { it.type == Rolletype.BARN }.mapNotNull { rolle ->
                    val stønadsid =
                        StønadEngangsbeløpId(
                            rolle.fødselsnummer!!,
                            stønad.skyldner,
                            stønad.sak,
                            engangsbeløptype = stønad.type,
                        )
                    erForskuddetRedusert(vedtakInfo, stønadsid, stønad.mottaker)
                }
            }
    }

    private fun erForskuddRedusertEtterFattetBidrag(vedtakInfo: SisteManuelleVedtak): List<ForskuddRedusertResultat> =
        vedtakInfo.vedtak.stønadsendringListe
            .filter { it.erBidrag }
            .filter {
                if (it.erDirekteAvslag()) {
                    LOGGER.info {
                        "Bidrag vedtaket ${vedtakInfo.vedtaksId} med type ${it.type} for kravhaver ${it.kravhaver} er direkte avslag med resultat ${it.enesteResultatkode()} og har derfor ingen inntekter."
                    }
                    false
                } else {
                    true
                }
            }.groupBy { it.sak }
            .flatMap { (sak, stønader) ->
                val stønad = stønader.first()
                val sak = bidragSakConsumer.hentSak(sak.verdi)
                val bidragsmottaker = sak.roller.find { it.type == Rolletype.BIDRAGSMOTTAKER }!!
                sak.roller.filter { it.type == Rolletype.BARN }.mapNotNull { rolle ->
                    val stønadsid =
                        StønadEngangsbeløpId(
                            rolle.fødselsnummer!!,
                            stønad.skyldner,
                            stønad.sak,
                            stønadstype = stønad.type,
                        )
                    erForskuddetRedusert(vedtakInfo, stønadsid, bidragsmottaker.fødselsnummer!!)
                }
            }

    private fun erForskuddetRedusert(
        vedtakFattet: SisteManuelleVedtak,
        stønadEngangsbeløpId: StønadEngangsbeløpId,
        mottaker: Personident,
    ): ForskuddRedusertResultat? {
        val gjelderBarn = stønadEngangsbeløpId.kravhaver
        val sistePeriode = hentLøpendeForskudd(stønadEngangsbeløpId.sak.verdi, gjelderBarn.verdi) ?: return null

        val vedtakForskudd =
            hentSisteManuelleForskuddVedtak(sistePeriode.vedtaksid, stønadEngangsbeløpId.sak, gjelderBarn)
                ?: return null
        val (beregnetForskudd, grunnlagsliste) = beregnForskudd(vedtakFattet.vedtak, vedtakForskudd.vedtak, gjelderBarn)

        val beregnetResultat = beregnetForskudd.resultat
        val beløpLøpende = sistePeriode.beløp!!
        val erForskuddRedusert = beløpLøpende > beregnetResultat.belop
        val sisteInntektForskudd =
            GrunnlagMapper.hentSisteDelberegningInntektFraForskudd(
                vedtakForskudd.vedtak,
                stønadEngangsbeløpId.kravhaver,
            )
        val sisteInntektFattetVedtak =
            GrunnlagMapper.hentSisteInntektFraBeregning(
                beregnetForskudd.grunnlagsreferanseListe,
                grunnlagsliste,
            )
        return erForskuddRedusert.ifTrue { _ ->
            LOGGER.info {
                """Forskudd er redusert i sak ${stønadEngangsbeløpId.sak.verdi} for bidragsmottaker ${mottaker.verdi} og barn ${gjelderBarn.verdi}. 
                   Løpende forskudd er $beløpLøpende og forskudd ble beregnet til ${beregnetResultat.belop} basert på siste vurdert inntekt fra bidrag vedtak ${vedtakFattet.vedtaksId} og grunnlag fra forskudd vedtak ${vedtakForskudd.vedtaksId}.
                   Siste løpende inntekt for BM i fattet vedtak er ${sisteInntektFattetVedtak?.totalinntekt} og siste inntekt for BM i forskudd vedtaket er ${sisteInntektForskudd?.totalinntekt}.
                   
                   Innteksdetaljer fra fattet vedtak $sisteInntektFattetVedtak 
                   og fra forskudd vedtaket $sisteInntektForskudd
                """.trimMargin()
                    .trimIndent()
                    .replace("\t", "")
                    .replace("  ", "")
            }
            ForskuddRedusertResultat(
                saksnummer = stønadEngangsbeløpId.sak.verdi,
                bidragsmottaker = mottaker.verdi,
                gjelderBarn = gjelderBarn.verdi,
                stønadstype = stønadEngangsbeløpId.stønadstype,
                engangsbeløptype = stønadEngangsbeløpId.engangsbeløptype,
            )
        } ?: run {
            LOGGER.info {
                """Forskudd er IKKE redusert i sak ${stønadEngangsbeløpId.sak.verdi} for bidragsmottaker ${mottaker.verdi} og barn ${gjelderBarn.verdi}. 
                   Løpende forskudd er $beløpLøpende og forskudd ble beregnet til ${beregnetResultat.belop} basert på siste vurdert inntekt fra bidrag vedtak ${vedtakFattet.vedtaksId} og grunnlag fra forskudd vedtak ${vedtakForskudd.vedtaksId}.
                   Siste løpende inntekt for BM i fattet vedtak er $sisteInntektFattetVedtak og siste inntekt for BM i forskudd vedtaket er $sisteInntektForskudd.
                   
                   Innteksdetaljer fra fattet vedtak $sisteInntektFattetVedtak 
                   og fra forskudd vedtaket $sisteInntektForskudd
                """.trimMargin()
                    .trimIndent()
                    .trimIndent()
                    .replace("\t", "")
                    .replace("  ", "")
            }
            null
        }
    }

    private fun hentVedtak(vedtakId: Int): VedtakDto? {
        val vedtak = bidragVedtakConsumer.hentVedtak(vedtakId) ?: return null
        if (vedtak.erDelvedtak || (vedtak.erOrkestrertVedtak && vedtak.type == Vedtakstype.INNKREVING)) return null
        val faktiskVedtak =
            if (vedtak.erOrkestrertVedtak) {
                bidragVedtakConsumer.hentVedtak(vedtak.referertVedtaksid!!)
            } else {
                vedtak
            } ?: return null
        if (faktiskVedtak.grunnlagListe.isEmpty()) {
            LOGGER.info {
                "Vedtak $vedtakId fattet av system ${vedtak.kildeapplikasjon} mangler grunnlag. Gjør ingen vurdering"
            }
            return null
        }
        return faktiskVedtak
    }

    private fun hentSisteManuelleForskuddVedtak(
        vedtakId: Int,
        saksnummer: Saksnummer,
        gjelderBarn: Personident,
    ): SisteManuelleVedtak? {
        val vedtak =
            bidragVedtakConsumer.hentVedtak(vedtakId)?.let {
                if (it.erIndeksreguleringEllerAldersjustering()) {
                    val forskuddVedtakISak =
                        bidragVedtakConsumer.hentVedtakForStønad(
                            HentVedtakForStønadRequest(
                                saksnummer,
                                Stønadstype.FORSKUDD,
                                personidentNav,
                                gjelderBarn,
                            ),
                        )
                    val sisteManuelleVedtak =
                        vedtaksFilter.finneSisteManuelleVedtak(
                            forskuddVedtakISak.vedtakListe,
                        ) ?: return null
                    bidragVedtakConsumer.hentVedtak(sisteManuelleVedtak.vedtaksid)?.let {
                        SisteManuelleVedtak(sisteManuelleVedtak.vedtaksid, it)
                    }
                } else {
                    SisteManuelleVedtak(vedtakId, it)
                }
            } ?: return null

        if (vedtak.vedtak.grunnlagListe.isEmpty()) {
            LOGGER.info {
                "Forskudd vedtak $vedtakId fattet av system ${vedtak.vedtak.kildeapplikasjon} mangler grunnlag. Gjør ingen vurdering"
            }
            return null
        }
        LOGGER.info { "Fant siste manuelle forskudd vedtak ${vedtak.vedtaksId}" }
        return vedtak
    }

    private fun beregnForskudd(
        vedtakBidrag: VedtakDto,
        vedtakLøpendeForskudd: VedtakDto,
        gjelderBarn: Personident,
    ): Pair<ResultatPeriode, List<GrunnlagDto>> {
        val grunnlag = GrunnlagMapper.byggGrunnlagForBeregning(vedtakBidrag, vedtakLøpendeForskudd, gjelderBarn)
        val resultat =
            beregning.beregn(
                BeregnGrunnlag(
                    periode =
                        ÅrMånedsperiode(
                            YearMonth.now(),
                            YearMonth.now().plusMonths(1),
                        ),
                    stønadstype = Stønadstype.FORSKUDD,
                    søknadsbarnReferanse = grunnlag.søknadsbarn.find { it.personIdent == gjelderBarn.verdi }!!.referanse,
                    grunnlagListe = grunnlag,
                ),
            )

        val sistePeriode = resultat.beregnetForskuddPeriodeListe.last()
        return sistePeriode to resultat.grunnlagListe
    }

    private fun hentLøpendeForskudd(
        saksnummer: String,
        gjelderBarn: String,
    ): StønadPeriodeDto? {
        val forskuddStønad =
            hentForskuddForSak(saksnummer, gjelderBarn) ?: run {
                LOGGER.info { "Fant ingen løpende forskudd i sak $saksnummer for barn $gjelderBarn" }
                return null
            }
        return forskuddStønad.periodeListe.hentSisteLøpendePeriode() ?: run {
            LOGGER.info {
                "Forskudd i sak $saksnummer og barn $gjelderBarn har opphørt før dagens dato. Det finnes ingen løpende forskudd"
            }
            null
        }
    }

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

    private fun List<StønadPeriodeDto>.hentSisteLøpendePeriode() =
        maxByOrNull { it.periode.fom }?.takeIf { it.periode.til == null || it.periode.til!!.isAfter(YearMonth.now()) }

    fun opprettOppgave(revurderingForskudd: RevurderingForskudd): Int {
        val oppgaveId = oppgaveService.opprettOppgaveForTilbakekrevingAvForskudd(revurderingForskudd)

        revurderingForskudd.oppgave = oppgaveId
        revurderingForskuddRepository.save(revurderingForskudd)
        return oppgaveId
    }
}
