package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import com.fasterxml.jackson.databind.node.POJONode
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.mapper.VedtakMapper
import no.nav.bidrag.automatiskjobb.mapper.tilOpprettGrunnlagRequestDto
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.beregn.barnebidrag.service.external.VedtakService
import no.nav.bidrag.beregn.barnebidrag.utils.hentSisteLøpendePeriode
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.indeksregulering.BeregnIndeksreguleringApi
import no.nav.bidrag.indeksregulering.bo.BeregnIndeksreguleringGrunnlag
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadPeriodeDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.BeløpshistorikkGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.BeløpshistorikkPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.InnholdMedReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningIndeksregulering
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettPeriodeRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettStønadsendringRequestDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Year

private val LOGGER = KotlinLogging.logger { }

/**
 * Beregner indeksregulering av bidrag og oppretter vedtaksforslag.
 *
 * Vedtaksforslagene fattes av en egen batch, se FattVedtakIndeksreguleringBidragBatch.
 */
@Service
@Import(BeregnIndeksreguleringApi::class)
class GjennomførIndeksreguleringBidragService(
    private val beregnIndeksreguleringApi: BeregnIndeksreguleringApi,
    private val beløpshistorikkConsumer: BidragBeløpshistorikkConsumer,
    private val personConsumer: BidragPersonConsumer,
    private val sakConsumer: BidragSakConsumer,
    private val bidragVedtakConsumer: BidragVedtakConsumer,
    private val vedtakService: VedtakService,
    private val vedtakMapper: VedtakMapper,
) {
    fun gjennomførIndeksregulering(
        indeksregulering: Indeksregulering,
        simuler: Boolean,
    ): Indeksregulering {
        val barn = indeksregulering.barn
        if (barn.skyldner.isNullOrBlank()) {
            LOGGER.warn { "Indeksregulering ${indeksregulering.id} i sak ${barn.saksnummer} mangler skyldner. Indeksreguleres ikke." }
            return indeksregulering.oppdaterIkkeGjennomført(Behandlingstype.FEILET, "MANGLER_SKYLDNER", simuler, Status.FEILET)
        }

        val stønadsid = barn.tilStønadsid(indeksregulering.stønadstype)

        val stønad = hentStønad(stønadsid)
        val løpendePeriode = stønad?.periodeListe?.hentSisteLøpendePeriode()
        if (stønad == null || løpendePeriode == null) {
            LOGGER.info { "Stønad $stønadsid er ikke løpende. Indeksreguleres ikke." }
            return indeksregulering.oppdaterIkkeGjennomført(Behandlingstype.INGEN, "INGEN_LØPENDE_STØNAD", simuler)
        }

        // Det skal ikke indeksreguleres noe som løper i annet enn NOK.
        if (!løpendePeriode.løperINorskeKroner()) {
            LOGGER.info { "Stønad $stønadsid løper i valuta ${løpendePeriode.valutakode}. Indeksreguleres ikke automatisk." }
            return indeksregulering.oppdaterIkkeGjennomført(Behandlingstype.MANUELL, "LØPER_I_UTENLANDSK_VALUTA", simuler)
        }

        val personobjekter = opprettPersonobjekter(indeksregulering, stønadsid)
        val beregningsresultat =
            beregnIndeksreguleringApi.beregnIndeksregulering(
                BeregnIndeksreguleringGrunnlag(
                    indeksregulerÅr = Year.of(indeksregulering.år),
                    stønadsid = stønadsid,
                    personobjektListe = personobjekter,
                    beløpshistorikkListe =
                        listOf(
                            stønad.tilBeløpshistorikkGrunnlag(indeksregulering.stønadstype, stønadsid, personobjekter),
                        ),
                    opphørsdato = løpendePeriode.periode.til,
                ),
            )

        val sluttberegning =
            beregningsresultat
                .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningIndeksregulering>(
                    grunnlagType = Grunnlagstype.SLUTTBEREGNING_INDEKSREGULERING,
                ).firstOrNull()
                ?: error("Beregning av indeksregulering for stønad $stønadsid ga ingen sluttberegning")

        if (sluttberegning.innhold.beløp.erIUtenlandskValuta()) {
            LOGGER.info { "Beregnet beløp for stønad $stønadsid er i utenlandsk valuta. Indeksreguleres ikke automatisk." }
            return indeksregulering.oppdaterIkkeGjennomført(Behandlingstype.MANUELL, "LØPER_I_UTENLANDSK_VALUTA", simuler)
        }

        val vedtaksforslagRequest = opprettVedtaksforslagRequest(indeksregulering, stønadsid, beregningsresultat, sluttberegning)

        val vedtaksid =
            if (simuler) {
                LOGGER.info { "Simulering: Oppretter ikke vedtaksforslag for indeksregulering av stønad $stønadsid." }
                null
            } else {
                bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(vedtaksforslagRequest)
            }

        return indeksregulering.also {
            it.vedtak = vedtaksid
            it.beløp = sluttberegning.innhold.beløp.verdi
            it.status = if (simuler) Status.SIMULERT else Status.BEHANDLET
            it.behandlingstype = Behandlingstype.FATTET_FORSLAG
            it.begrunnelse = emptyList()
        }
    }

    private fun opprettVedtaksforslagRequest(
        indeksregulering: Indeksregulering,
        stønadsid: Stønadsid,
        beregningsresultat: List<GrunnlagDto>,
        sluttberegning: InnholdMedReferanse<SluttberegningIndeksregulering>,
    ): OpprettVedtakRequestDto {
        val sak = sakConsumer.hentSak(indeksregulering.barn.saksnummer)
        val sakrolleBarn = vedtakMapper.hentBarn(sak, indeksregulering.barn.kravhaver)
        val mottaker =
            vedtakMapper.reellMottakerEllerBidragsmottaker(sakrolleBarn, sak.roller)
                ?: error("Fant ikke mottaker for stønad $stønadsid")

        return OpprettVedtakRequestDto(
            type = Vedtakstype.INDEKSREGULERING,
            vedtakstidspunkt = null,
            unikReferanse = indeksregulering.unikReferanse,
            enhetsnummer = Enhetsnummer("9999"),
            engangsbeløpListe = emptyList(),
            behandlingsreferanseListe = emptyList(),
            kilde = Vedtakskilde.AUTOMATISK,
            grunnlagListe = beregningsresultat.toSet().map { it.tilOpprettGrunnlagRequestDto() },
            stønadsendringListe =
                listOf(
                    OpprettStønadsendringRequestDto(
                        type = indeksregulering.stønadstype,
                        sak = stønadsid.sak,
                        kravhaver = stønadsid.kravhaver,
                        skyldner = stønadsid.skyldner,
                        mottaker = mottaker,
                        beslutning = Beslutningstype.ENDRING,
                        grunnlagReferanseListe = emptyList(),
                        innkreving = Innkrevingstype.MED_INNKREVING,
                        sisteVedtaksid = vedtakService.finnSisteVedtaksid(stønadsid),
                        førsteIndeksreguleringsår =
                            sluttberegning.innhold.nesteIndeksreguleringsår?.value ?: (indeksregulering.år + 1),
                        periodeListe =
                            listOf(
                                OpprettPeriodeRequestDto(
                                    periode = sluttberegning.innhold.periode,
                                    beløp = sluttberegning.innhold.beløp.verdi,
                                    valutakode = Valutakode.NOK.name,
                                    resultatkode = Resultatkode.INDEKSREGULERING.name,
                                    grunnlagReferanseListe = listOf(sluttberegning.referanse),
                                ),
                            ),
                    ),
                ),
        )
    }

    private fun opprettPersonobjekter(
        indeksregulering: Indeksregulering,
        stønadsid: Stønadsid,
    ): List<GrunnlagDto> =
        listOf(
            stønadsid.kravhaver.tilPersonGrunnlag(
                Grunnlagstype.PERSON_SØKNADSBARN,
                "${Grunnlagstype.PERSON_SØKNADSBARN}_${stønadsid.toReferanse()}",
                indeksregulering.barn.fødselsdato,
            ),
            stønadsid.skyldner.tilPersonGrunnlag(
                Grunnlagstype.PERSON_BIDRAGSPLIKTIG,
                "${Grunnlagstype.PERSON_BIDRAGSPLIKTIG}_${stønadsid.skyldner.verdi}",
            ),
        )

    private fun Personident.tilPersonGrunnlag(
        grunnlagstype: Grunnlagstype,
        referanse: String,
        fødselsdato: LocalDate? = null,
    ): GrunnlagDto {
        val person = personConsumer.hentPerson(this)
        return GrunnlagDto(
            referanse = referanse,
            type = grunnlagstype,
            innhold =
                POJONode(
                    Person(
                        ident = this,
                        fødselsdato = fødselsdato ?: person.fødselsdato ?: LocalDate.MIN,
                        navn = person.navn,
                    ),
                ),
        )
    }

    private fun hentStønad(stønadsid: Stønadsid): StønadDto? =
        beløpshistorikkConsumer.hentLøpendeStønad(
            HentStønadRequest(
                type = stønadsid.type,
                sak = stønadsid.sak,
                skyldner = stønadsid.skyldner,
                kravhaver = stønadsid.kravhaver,
            ),
        )

    private fun StønadDto.tilBeløpshistorikkGrunnlag(
        stønadstype: Stønadstype,
        stønadsid: Stønadsid,
        personobjekter: List<GrunnlagDto>,
    ): GrunnlagDto {
        val beløpshistorikkGrunnlagstype =
            when (stønadstype) {
                Stønadstype.BIDRAG -> Grunnlagstype.BELØPSHISTORIKK_BIDRAG
                Stønadstype.BIDRAG18AAR -> Grunnlagstype.BELØPSHISTORIKK_BIDRAG_18_ÅR
                Stønadstype.OPPFOSTRINGSBIDRAG -> Grunnlagstype.BELØPSHISTORIKK_OPPFOSTRINGSBIDRAG
                else -> error("Ugyldig stønadstype: $stønadstype")
            }

        return GrunnlagDto(
            referanse = "${beløpshistorikkGrunnlagstype}_${stønadsid.toReferanse()}",
            type = beløpshistorikkGrunnlagstype,
            gjelderReferanse = personobjekter.first { it.type == Grunnlagstype.PERSON_BIDRAGSPLIKTIG }.referanse,
            gjelderBarnReferanse = personobjekter.first { it.type == Grunnlagstype.PERSON_SØKNADSBARN }.referanse,
            innhold =
                POJONode(
                    BeløpshistorikkGrunnlag(
                        nesteIndeksreguleringsår = nesteIndeksreguleringsår ?: førsteIndeksreguleringsår,
                        beløpshistorikk =
                            periodeListe.map {
                                BeløpshistorikkPeriode(
                                    periode = it.periode,
                                    beløp = it.beløp,
                                    valutakode = it.valutakode,
                                    vedtaksid = it.vedtaksid,
                                )
                            },
                    ),
                ),
        )
    }

    // Vi antar at stønader fra gammel løsning som ikke har satt valutakode løper i norske kroner. Ny løsning skal ha satt valutakode.
    private fun StønadPeriodeDto.løperINorskeKroner(): Boolean = valutakode == null || valutakode == Valutakode.NOK.name

    private fun Indeksregulering.oppdaterIkkeGjennomført(
        behandlingstype: Behandlingstype,
        begrunnelse: String,
        simuler: Boolean,
        statusVedFeil: Status? = null,
    ): Indeksregulering =
        also {
            it.behandlingstype = behandlingstype
            it.begrunnelse = listOf(begrunnelse)
            it.status = statusVedFeil ?: if (simuler) Status.SIMULERT else Status.BEHANDLET
        }
}
