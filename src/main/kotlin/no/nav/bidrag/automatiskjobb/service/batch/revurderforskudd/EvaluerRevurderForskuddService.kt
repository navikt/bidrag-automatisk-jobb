package no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd

import com.fasterxml.jackson.databind.node.POJONode
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragGrunnlagConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.mapper.VedtakMapper
import no.nav.bidrag.automatiskjobb.mapper.tilOpprettGrunnlagRequestDto
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.service.ReskontroService
import no.nav.bidrag.beregn.barnebidrag.service.external.SisteManuelleVedtak
import no.nav.bidrag.beregn.barnebidrag.service.external.VedtakService
import no.nav.bidrag.beregn.barnebidrag.utils.hentSisteLøpendePeriode
import no.nav.bidrag.beregn.core.exception.UgyldigInputException
import no.nav.bidrag.beregn.forskudd.BeregnForskuddApi
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
import no.nav.bidrag.domene.enums.vedtak.VirkningstidspunktÅrsakstype
import no.nav.bidrag.domene.felles.personidentNav
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.domene.tid.Datoperiode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.inntekt.InntektApi
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.beregning.forskudd.BeregnetForskuddResultat
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.InnhentetAinntekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.VirkningstidspunktGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.bidragsmottaker
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPersonMedIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.opprettAinntektGrunnlagsreferanse
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
        beregnFraMåned: YearMonth,
        antallMånederForBeregning: Long,
    ): RevurderingForskudd? {
        val grunnlagsliste = mutableListOf<GrunnlagDto>()
        val stønadsendringer = mutableListOf<OpprettStønadsendringRequestDto>()
        var grunnlag: HentGrunnlagDto? = null

        LOGGER.info {
            "Starter evaluering av revudering forskudd for sak ${revurderingForskudd.saksnummer} " +
                "med barn ${revurderingForskudd.barn.joinToString(separator = ", ") { it.kravhaver }}"
        }

        revurderingForskudd.barn.forEach { barn ->
            val sisteManuelleVedtak = finnSisteManuelleVedtak(barn)
            if (sisteManuelleVedtak == null) {
                LOGGER.info {
                    "Fant ingen manuelle vedtak for barn ${barn.kravhaver} i sak ${barn.saksnummer}. \nBeregner ikke revurdering av forskudd."
                }
                revurderingForskudd.behandlingstype = Behandlingstype.INGEN
                revurderingForskudd.status = if (simuler) Status.SIMULERT else Status.BEHANDLET
                revurderingForskudd.begrunnelse = listOf("INGEN_MANUELLE_VEDTAK")
                return revurderingForskudd
            }

            val forskudd = hentForskuddForSak(barn.saksnummer, barn.kravhaver)
            if (forskudd == null || !erForskuddLøpende(forskudd)) {
                LOGGER.info {
                    "Forskudd i sak ${forskudd?.sak} er ikke løpende for revurderingForskudd $revurderingForskudd! \nBeregner ikke revurdering av forskudd."
                }
                revurderingForskudd.behandlingstype = Behandlingstype.INGEN
                revurderingForskudd.status = if (simuler) Status.SIMULERT else Status.BEHANDLET
                revurderingForskudd.begrunnelse = listOf("FORSKUDD_IKKE_LØPENDE")
                return revurderingForskudd
            }

            val løpendeBeløp = forskudd.periodeListe.hentSisteLøpendePeriode()!!.beløp!!

            val barnGrunnlagReferanse =
                sisteManuelleVedtak.vedtak.grunnlagListe
                    .hentPersonMedIdent(
                        barn.kravhaver,
                    )?.referanse

            if (barnGrunnlagReferanse == null) {
                LOGGER.warn {
                    "Fant ingen grunnlag for barn ${barn.kravhaver} i sak ${barn.saksnummer}. " +
                        "\nDette skal ikke forekomme! Beregner ikke revurdering av forskudd."
                }
                revurderingForskudd.behandlingstype = Behandlingstype.FEILET
                revurderingForskudd.status = if (simuler) Status.SIMULERT else Status.FEILET
                revurderingForskudd.begrunnelse = listOf("FANT_INGEN_GRUNNLAG_FOR_BARN")
                return revurderingForskudd
            }

            val bmGrunnlagReferanse =
                sisteManuelleVedtak.vedtak.grunnlagListe.bidragsmottaker
                    ?.referanse

            if (bmGrunnlagReferanse == null) {
                LOGGER.warn {
                    "Fant ingen grunnlag for bidragsmottaker for barn ${barn.kravhaver} i sak " +
                        "${barn.saksnummer}. \nDette skal ikke forekomme! Beregner ikke revurdering av forskudd."
                }
                revurderingForskudd.behandlingstype = Behandlingstype.FEILET
                revurderingForskudd.status = if (simuler) Status.SIMULERT else Status.FEILET
                revurderingForskudd.begrunnelse = listOf("FANT_INGEN_GRUNNLAG_FOR_BIDRAGSMOTTAKER")
                return revurderingForskudd
            }

            // Filterer ut grunnlag som gjelder andre barn enn det som revurderes
            val filtrertGrunnlagForBarn =
                sisteManuelleVedtak.vedtak.grunnlagListe
                    .filter {
                        it.gjelderBarnReferanse == null ||
                            it.gjelderBarnReferanse == barnGrunnlagReferanse
                    }.toMutableList()

            val påkrevdeGrunnlagstyper =
                setOf(
                    Grunnlagstype.BOSTATUS_PERIODE,
                    Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                    Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                    Grunnlagstype.PERSON_SØKNADSBARN,
                    Grunnlagstype.SIVILSTAND_PERIODE,
                )

            val andreGrunnlagstyper =
                setOf(
                    Grunnlagstype.PERSON_HUSSTANDSMEDLEM,
                    Grunnlagstype.INNHENTET_HUSSTANDSMEDLEM,
                    Grunnlagstype.INNHENTET_SIVILSTAND,
                )

            val relevantGrunnlag =
                filtrertGrunnlagForBarn
                    .filter {
                        påkrevdeGrunnlagstyper.contains(it.type) || andreGrunnlagstyper.contains(it.type)
                    }.toMutableList()

            val eksisterendeGrunnlagstyper = relevantGrunnlag.map { it.type }.toSet()
            val manglendeGrunnlagstyper = påkrevdeGrunnlagstyper - eksisterendeGrunnlagstyper

            if (manglendeGrunnlagstyper.isNotEmpty()) {
                LOGGER.warn {
                    "Manglende grunnlagstyper for barn ${barn.kravhaver} i sak ${barn.saksnummer}: ${manglendeGrunnlagstyper.joinToString()}"
                }
                revurderingForskudd.behandlingstype = Behandlingstype.FEILET
                revurderingForskudd.status = if (simuler) Status.SIMULERT else Status.FEILET
                revurderingForskudd.begrunnelse =
                    listOf("MANGLENDE_GRUNNLAGSTYPER: ${manglendeGrunnlagstyper.joinToString()}")
                return revurderingForskudd
            }

            // Oppretter grunnlag for virkningstidspunkt
            relevantGrunnlag.add(opprettVirkningstidspunkt(forskudd.mottaker.verdi, beregnFraMåned))

            // Henter inntektsgrunnlaget for forskuddet
            try {
                if (grunnlag == null) { // Henter grunnlaget for inntekt på BM kun en gang per sak
                    grunnlag = hentInntektsGrunnlagForForskudd(forskudd)
                }
            } catch (e: Exception) {
                LOGGER.warn(e) {
                    "Feil ved henting av inntektsgrunnlag for revurdering av forskudd for barn ${barn.kravhaver} i sak ${barn.saksnummer}"
                }
                revurderingForskudd.behandlingstype = Behandlingstype.FEILET
                revurderingForskudd.status = if (simuler) Status.SIMULERT else Status.FEILET
                revurderingForskudd.begrunnelse = listOf("FEIL_VED_HENTING_AV_INNTEKTSGRUNNLAG: ${e.message}")
                return revurderingForskudd
            }

            // Legger til INNHETET_INNTEKT_AINNTEKT på grunnlaget
            val hentetTidspunkt = LocalDateTime.now()
            val innhentetGrunnlagForBM =
                grunnlag.ainntektListe.tilGrunnlagsobjekt(
                    hentetTidspunkt,
                    bmGrunnlagReferanse,
                )
            relevantGrunnlag.add(innhentetGrunnlagForBM)

            // Finner inntekter for forskuddsmottaker fra grunnlaget
            val transformerteInntekter = finnInntekterForForskuddFraGrunnlaget(grunnlag)

            val månedsinntektGangetTolv =
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
                transformerteInntekter.summertÅrsinntektListe
                    .find {
                        it.inntektRapportering == Inntektsrapportering.AINNTEKT_BEREGNET_12MND
                    }?.sumInntekt
                    ?: beregn12MånedsInntekt(transformerteInntekter) // Fallback om årsinntekt er null (skal ikke skje)

            var inntekt: BigDecimal
            var inntektsrapportering: Inntektsrapportering

            if (årsinntekt >= (månedsinntektGangetTolv ?: BigDecimal.ZERO)) {
                inntekt = årsinntekt
                inntektsrapportering = Inntektsrapportering.AINNTEKT_BEREGNET_12MND
            } else {
                inntekt = månedsinntektGangetTolv!!
                inntektsrapportering = Inntektsrapportering.AINNTEKT_BEREGNET_LAVESTE_MÅNEDSINNTEKT_SISTE_3MND_GANGET_TOLV
            }

            // TODO(Barnetrygd, utvidet barnetrygd, småbarnstillegg og kontaktstøtte?)

            val beregnetForskudd: BeregnetForskuddResultat =
                try {
                    beregnNyttForskudd(
                        relevantGrunnlag,
                        beregnFraMåned,
                        barnGrunnlagReferanse,
                        bmGrunnlagReferanse,
                        inntekt,
                        inntektsrapportering,
                        innhentetGrunnlagForBM.referanse,
                    )
                } catch (e: IllegalArgumentException) {
                    LOGGER.error(e) {
                        "Feil ved beregning av revurdering forskudd for barn ${barn.kravhaver} i sak ${barn.saksnummer}"
                    }
                    revurderingForskudd.behandlingstype = Behandlingstype.FEILET
                    revurderingForskudd.status = if (simuler) Status.SIMULERT else Status.FEILET
                    revurderingForskudd.begrunnelse = listOf("FEIL_VED_BEREGNING: ${e.message}")
                    return revurderingForskudd
                } catch (e: UgyldigInputException) {
                    LOGGER.error(e) {
                        "Ugyldig input ved beregning av revurdering forskudd for barn ${barn.kravhaver} i sak ${barn.saksnummer}"
                    }
                    revurderingForskudd.behandlingstype = Behandlingstype.FEILET
                    revurderingForskudd.status = if (simuler) Status.SIMULERT else Status.FEILET
                    revurderingForskudd.begrunnelse = listOf("UGYLDIG_INPUT_VED_BEREGNING: ${e.message}")
                    return revurderingForskudd
                } catch (e: Exception) {
                    LOGGER.error(e) {
                        "Ukjent feil ved beregning av revurdering forskudd for barn ${barn.kravhaver} i sak ${barn.saksnummer}"
                    }
                    revurderingForskudd.behandlingstype = Behandlingstype.FEILET
                    revurderingForskudd.status = if (simuler) Status.SIMULERT else Status.FEILET
                    revurderingForskudd.begrunnelse = listOf("UKJENT_FEIL_VED_BEREGNING: ${e.message}")
                    return revurderingForskudd
                }

            // Ingen endring i forskudd skal gjøres
            if (!skalForskuddSettesNed(løpendeBeløp, beregnetForskudd)) {
                LOGGER.info {
                    "Forskudd for barn ${barn.kravhaver} i sak ${barn.saksnummer} skal ikke settes ned etter revurdering."
                }
                revurderingForskudd.behandlingstype = Behandlingstype.INGEN
                revurderingForskudd.status = if (simuler) Status.SIMULERT else Status.BEHANDLET
                revurderingForskudd.begrunnelse = listOf("SKAL_IKKE_SETTES_NED")
                return@forEach
            }

            // Gjør en sjekk mot reskontro for å se om det eksisterer A4 transaksjoner (forskudd) for de siste 3 månedene. Dette gjøres for å kunne opprette oppgaver for tilbakekreving det er utbetalt forskudd
            try {
                if (bidragReskontroService.finnesForskuddForSakPeriode(
                        Saksnummer(barn.saksnummer),
                        listOf(
                            LocalDate.now().minusMonths(3),
                            LocalDate.now().minusMonths(2),
                            LocalDate.now().minusMonths(1),
                        ),
                    )
                ) {
                    revurderingForskudd.vurdereTilbakekreving = true
                }
            } catch (e: Exception) {
                LOGGER.error(e) {
                    "Feil ved sjekk av reskontro for revurdering forskudd i sak ${barn.saksnummer}"
                }
                revurderingForskudd.behandlingstype = Behandlingstype.FEILET
                revurderingForskudd.status = if (simuler) Status.SIMULERT else Status.FEILET
                revurderingForskudd.begrunnelse = listOf("FEIL_VED_SJEKK_AV_RESKONTRO: ${e.message}")
                return revurderingForskudd
            }

            revurderingForskudd.vedtaksidBeregning = sisteManuelleVedtak.vedtaksId

            val sak = bidragSakConsumer.hentSak(barn.saksnummer)
            val sakrolleBarn = vedtakMapper.hentBarn(sak, barn.kravhaver)
            val mottaker = vedtakMapper.reellMottakerEllerBidragsmottaker(sakrolleBarn, sak.roller)!!

            grunnlagsliste.addAll(relevantGrunnlag)
            grunnlagsliste.addAll(beregnetForskudd.grunnlagListe)

            stønadsendringer.add(
                OpprettStønadsendringRequestDto(
                    type = Stønadstype.FORSKUDD,
                    sak = Saksnummer(barn.saksnummer),
                    kravhaver = Personident(barn.kravhaver),
                    skyldner = Personident("NAV"),
                    mottaker = mottaker,
                    beslutning = Beslutningstype.ENDRING,
                    grunnlagReferanseListe = emptyList(),
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    sisteVedtaksid =
                        vedtakService.finnSisteVedtaksid(
                            revurderingForskudd.tilStønadsid(barn.kravhaver),
                        ),
                    førsteIndeksreguleringsår = YearMonth.now().plusYears(1).year,
                    periodeListe =
                        beregnetForskudd.beregnetForskuddPeriodeListe.map {
                            OpprettPeriodeRequestDto(
                                periode = it.periode,
                                beløp = it.resultat.belop,
                                valutakode = "NOK",
                                resultatkode = it.resultat.kode.name,
                                grunnlagReferanseListe = it.grunnlagsreferanseListe,
                            )
                        },
                ),
            )
        }

        if (stønadsendringer.isEmpty()) {
            return revurderingForskudd
        }

        val opprettVedtakRequestDto =
            opprettVedtaksforslag(
                revurderingForskudd,
                stønadsendringer,
                grunnlagsliste,
            )
        val vedtakId =
            if (simuler) {
                null
            } else {
                try {
                    bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(opprettVedtakRequestDto)
                } catch (e: Exception) {
                    LOGGER.error(e) {
                        "Feil ved oppretting av vedtaksforslag for revurdering av forskudd for sak " +
                            revurderingForskudd.saksnummer
                    }
                    revurderingForskudd.behandlingstype = Behandlingstype.FEILET
                    revurderingForskudd.status = Status.FEILET
                    revurderingForskudd.begrunnelse = listOf("FEIL_VED_OPPRETTING_AV_VEDTAKSFORSLAG: ${e.message}")
                    return revurderingForskudd
                }
            }
        revurderingForskudd.vedtak = vedtakId
        revurderingForskudd.status = if (simuler) Status.SIMULERT else Status.BEHANDLET
        revurderingForskudd.behandlingstype = Behandlingstype.FATTET_FORSLAG
        revurderingForskudd.begrunnelse = emptyList() // Tømmer, kan være fylt pga tidligere barn

        return revurderingForskudd
    }

    private fun beregn12MånedsInntekt(transformerteInntekter: TransformerInntekterResponse): BigDecimal =
        (12L downTo 1)
            .mapNotNull { i ->
                val måned = YearMonth.now().minusMonths(i)
                transformerteInntekter.summertMånedsinntektListe
                    .find { måned.equals(it.gjelderÅrMåned) }
                    ?.sumInntekt ?: BigDecimal.ZERO
            }.reduce { årsinntekt, månedsinntekt -> årsinntekt.add(månedsinntekt) }

    private fun opprettVedtaksforslag(
        revurderingForskudd: RevurderingForskudd,
        stønadsendringer: List<OpprettStønadsendringRequestDto>,
        grunnlag: MutableList<GrunnlagDto>,
    ): OpprettVedtakRequestDto =
        OpprettVedtakRequestDto(
            type = Vedtakstype.REVURDERING,
            vedtakstidspunkt = null,
            unikReferanse = revurderingForskudd.unikReferanse,
            enhetsnummer = Enhetsnummer("9999"),
            stønadsendringListe = stønadsendringer,
            engangsbeløpListe = emptyList(),
            behandlingsreferanseListe =
                listOf(
                    OpprettBehandlingsreferanseRequestDto(
                        kilde = BehandlingsrefKilde.REVURDERING_FORSKUDD,
                        referanse = revurderingForskudd.batchId,
                    ),
                ),
            grunnlagListe =
                grunnlag
                    .toSet()
                    .map { it.tilOpprettGrunnlagRequestDto() },
            // toSet() sørger for at duplikater fjernes.
            kilde = Vedtakskilde.AUTOMATISK,
        )

    private fun finnSisteManuelleVedtak(barn: Barn): SisteManuelleVedtak? =
        vedtakService.finnSisteManuelleVedtak(
            Stønadsid(
                Stønadstype.FORSKUDD,
                Personident(barn.kravhaver),
                Personident("NAV"),
                Saksnummer(barn.saksnummer),
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

    private fun finnInntekterForForskuddFraGrunnlaget(grunnlag: HentGrunnlagDto): TransformerInntekterResponse {
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
        grunnlag: MutableList<GrunnlagDto>,
        beregnFraMåned: YearMonth,
        barnGrunnlagReferanse: String,
        bmGrunnlagReferanse: String,
        inntekt: BigDecimal,
        inntektsrapportering: Inntektsrapportering,
        innhentetGrunnlagReferanse: String,
    ): BeregnetForskuddResultat {
        val periode = ÅrMånedsperiode(beregnFraMåned, null)
        grunnlag.removeIf { it.type == Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE }
        grunnlag.add(
            GrunnlagDto(
                type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                innhold =
                    POJONode(
                        InntektsrapporteringPeriode(
                            periode = periode,
                            manueltRegistrert = false,
                            inntektsrapportering = inntektsrapportering,
                            beløp = inntekt,
                            opprinneligPeriode = ÅrMånedsperiode(LocalDate.now().minusYears(1), null),
                            gjelderBarn = barnGrunnlagReferanse,
                            valgt = true,
                        ),
                    ),
                referanse = "${Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE}_${bmGrunnlagReferanse}_$periode",
                gjelderReferanse = bmGrunnlagReferanse,
                // Dette skal bare settes for Barnetillegg og Kontanstøtte inntektene
                gjelderBarnReferanse = null,
                grunnlagsreferanseListe = listOf(innhentetGrunnlagReferanse),
            ),
        )

        return beregnForskuddApi.beregn(
            BeregnGrunnlag(
                periode =
                    ÅrMånedsperiode(
                        beregnFraMåned,
                        beregnFraMåned.plusMonths(1),
                    ),
                stønadstype = Stønadstype.FORSKUDD,
                søknadsbarnReferanse = barnGrunnlagReferanse,
                grunnlagListe = grunnlag,
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

    private fun opprettVirkningstidspunkt(
        gjelderReferanse: String,
        beregnFraMåned: YearMonth,
    ) = GrunnlagDto(
        referanse = "virkningstidspunkt_$gjelderReferanse",
        type = Grunnlagstype.VIRKNINGSTIDSPUNKT,
        gjelderReferanse = gjelderReferanse,
        innhold =
            POJONode(
                VirkningstidspunktGrunnlag(
                    årsak = VirkningstidspunktÅrsakstype.REVURDERING_MÅNEDEN_ETTER,
                    virkningstidspunkt = beregnFraMåned.atDay(1),
                ),
            ),
    )

    private fun List<AinntektGrunnlagDto>.tilGrunnlagsobjekt(
        hentetTidspunkt: LocalDateTime,
        gjelderReferanse: String,
    ) = GrunnlagDto(
        referanse = opprettAinntektGrunnlagsreferanse(gjelderReferanse),
        type = Grunnlagstype.INNHENTET_INNTEKT_AINNTEKT,
        gjelderReferanse = gjelderReferanse,
        innhold =
            POJONode(
                InnhentetAinntekt(
                    hentetTidspunkt = hentetTidspunkt,
                    grunnlag =
                        map {
                            InnhentetAinntekt.AinntektInnhentet(
                                periode = Datoperiode(it.periodeFra, it.periodeTil),
                                ainntektspostListe =
                                    it.ainntektspostListe.map { post ->
                                        InnhentetAinntekt.Ainntektspost(
                                            utbetalingsperiode = post.utbetalingsperiode,
                                            opptjeningsperiodeFra = post.opptjeningsperiodeFra,
                                            opptjeningsperiodeTil = post.opptjeningsperiodeTil,
                                            kategori = post.kategori,
                                            fordelType = post.fordelType,
                                            beløp = post.beløp,
                                            etterbetalingsperiodeFra = post.etterbetalingsperiodeFra,
                                            etterbetalingsperiodeTil = post.etterbetalingsperiodeTil,
                                            beskrivelse = post.beskrivelse,
                                            opplysningspliktigId = post.opplysningspliktigId,
                                            virksomhetId = post.virksomhetId,
                                        )
                                    },
                            )
                        },
                ),
            ),
    )
}
