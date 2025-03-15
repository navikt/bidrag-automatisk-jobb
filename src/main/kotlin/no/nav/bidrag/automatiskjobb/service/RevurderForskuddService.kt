package no.nav.bidrag.automatiskjobb.service

import com.fasterxml.jackson.databind.node.POJONode
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragStønadConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.beregn.forskudd.BeregnForskuddApi
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.beregning.forskudd.ResultatBeregning
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Grunnlagsreferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.bidragsmottaker
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerBasertPåEgenReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnGrunnlagSomErReferertFraGrunnlagsreferanseListe
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentAllePersoner
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPersonMedIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.søknadsbarn
import no.nav.bidrag.transport.behandling.stonad.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.stonad.response.StønadDto
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.behandling.vedtak.response.StønadsendringDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakPeriodeDto
import no.nav.bidrag.transport.felles.ifTrue
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.YearMonth

data class ForskuddRedusertResultat(
    val saksnummer: String,
    val bidragsmottaker: String,
    val gjelderBarn: String,
)

private val LOGGER = KotlinLogging.logger {}

@Service
@Import(BeregnForskuddApi::class)
class RevurderForskuddService(
    private val bidragStønadConsumer: BidragStønadConsumer,
    private val bidragSakConsumer: BidragSakConsumer,
    private val bidragVedtakConsumer: BidragVedtakConsumer,
    private val beregning: BeregnForskuddApi,
) {
    fun erForskuddRedusert(vedtakHendelse: VedtakHendelse): List<ForskuddRedusertResultat> {
        val vedtakBidrag = bidragVedtakConsumer.hentVedtak(vedtakHendelse.id) ?: return listOf()
        return vedtakBidrag.stønadsendringListe
            .filter { it.type == Stønadstype.BIDRAG }
            .mapNotNull {
                val gjelderBarn = it.kravhaver
                val forskuddStønad = hentLøpendeForskuddForSak(it.sak.verdi, gjelderBarn.verdi) ?: return@mapNotNull null
                val sistePeriode =
                    forskuddStønad.periodeListe
                        .maxByOrNull { it.periode.fom }
                        ?.takeIf { it.periode.til == null || it.periode.til!!.isAfter(YearMonth.now()) }
                        ?: return@mapNotNull null

                val vedtakForskudd = bidragVedtakConsumer.hentVedtak(sistePeriode.vedtaksid)!!
                val beregnetForskudd = beregnForskudd(vedtakBidrag, vedtakForskudd, gjelderBarn)

                val forskuddStønadBarn = vedtakForskudd.stønadsendringListe.find { st -> st.kravhaver == gjelderBarn }!!
                val løpendeForskudd = forskuddStønadBarn.periodeListe.maxBy { it.periode.fom }
                val beløpLøpende = løpendeForskudd.beløp!!
                val erForskuddRedusert = beløpLøpende > beregnetForskudd.belop
                erForskuddRedusert.ifTrue { _ ->
                    LOGGER.info {
                        "Forskudd er redusert i sak ${it.sak.verdi}. Løpende forskudd er $beløpLøpende og ny beregnet forskudd skal være ${beregnetForskudd.belop}"
                    }
                    secureLogger.info {
                        """Forskudd er redusert i sak ${it.sak.verdi} for bidragsmottaker ${it.mottaker.verdi} og barn ${gjelderBarn.verdi}
                            Løpende forskudd er $beløpLøpende og ny beregnet forskudd skal være ${beregnetForskudd.belop}"""
                    }
                    ForskuddRedusertResultat(
                        saksnummer = it.sak.verdi,
                        bidragsmottaker = it.mottaker.verdi,
                        gjelderBarn = gjelderBarn.verdi,
                    )
                }
            }
    }

    private fun beregnForskudd(
        vedtakBidrag: VedtakDto,
        vedtakLøpendeForskudd: VedtakDto,
        gjelderBarn: Personident,
    ): ResultatBeregning {
        val grunnlag = byggGrunnlagForBeregning(vedtakBidrag, vedtakLøpendeForskudd, gjelderBarn)
        val resultat =
            beregning.beregn(
                BeregnGrunnlag(
                    periode =
                        ÅrMånedsperiode(
                            YearMonth.now(),
                            YearMonth.now().plusMonths(1),
                        ),
                    stønadstype = Stønadstype.FORSKUDD,
                    søknadsbarnReferanse = grunnlag.søknadsbarn.first().referanse,
                    grunnlagListe = grunnlag,
                ),
            )

        val sistePeriode = resultat.beregnetForskuddPeriodeListe.last()
        return sistePeriode.resultat
    }

    private fun byggGrunnlagForBeregning(
        vedtakBidrag: VedtakDto,
        vedtakLøpendeForskudd: VedtakDto,
        gjelderBarn: Personident,
    ): List<GrunnlagDto> {
        val bidragStønadBarn = vedtakBidrag.stønadsendringListe.find { it.kravhaver == gjelderBarn }!!
        val grunnlagBidrag = hentGrunnlagFraBidrag(vedtakBidrag, bidragStønadBarn)

        val forskuddStønadBarn = vedtakLøpendeForskudd.stønadsendringListe.find { it.kravhaver == gjelderBarn }!!
        val grunnlagForskudd = hentGrunnlagFraForskudd(vedtakLøpendeForskudd, forskuddStønadBarn)

        val personobjekter = vedtakLøpendeForskudd.grunnlagListe.hentAllePersoner() as List<GrunnlagDto>

        val søknad = vedtakLøpendeForskudd.grunnlagListe.filtrerBasertPåEgenReferanse(Grunnlagstype.SØKNAD).first() as GrunnlagDto
        val bidragsmottaker = personobjekter.bidragsmottaker!!
        val søknadsbarn = vedtakLøpendeForskudd.grunnlagListe.hentPersonMedIdent(gjelderBarn.verdi)!!
        val grunnlagBidragJustert =
            grunnlagBidrag.map {
                it.copy(
                    gjelderReferanse = bidragsmottaker.referanse,
                    gjelderBarnReferanse = if (it.gjelderBarnReferanse != null) søknadsbarn.referanse else null,
                    innhold =
                        if (it.type == Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE) {
                            val inntektInnhold = it.innholdTilObjekt<InntektsrapporteringPeriode>()
                            POJONode(
                                inntektInnhold.copy(
                                    gjelderBarn = søknadsbarn.referanse,
                                ),
                            )
                        } else {
                            it.innhold
                        },
                )
            }
        return (grunnlagBidragJustert + grunnlagForskudd + listOf(søknad) + personobjekter).toList()
    }

    private fun hentGrunnlagFraBidrag(
        vedtak: VedtakDto,
        stønadsendring: StønadsendringDto,
    ): List<GrunnlagDto> {
        val periode = stønadsendring.periodeListe.filter { it.resultatkode != Resultatkode.OPPHØR.name }.maxBy { it.periode.fom }
        val bidragsmottaker = vedtak.grunnlagListe.bidragsmottaker!!
        val søknadsbarn = vedtak.grunnlagListe.hentPersonMedIdent(stønadsendring.kravhaver.verdi)!!
        val inntekter = vedtak.grunnlagListe.hentInntekter(periode, bidragsmottaker.referanse, søknadsbarn.referanse)
        return inntekter
    }

    private fun hentGrunnlagFraForskudd(
        vedtak: VedtakDto,
        stønadsendring: StønadsendringDto,
    ): List<GrunnlagDto> {
        val periode = stønadsendring.periodeListe.maxBy { it.periode.fom }
        val bidragsmottaker = vedtak.grunnlagListe.bidragsmottaker!!
        val sivilstandPerioder = vedtak.grunnlagListe.hentSivilstandPerioder(periode, bidragsmottaker.referanse)
        val barnIHusstandPerioder = vedtak.grunnlagListe.hentBarnIHusstandPerioder(periode)
        return sivilstandPerioder + barnIHusstandPerioder
    }

    private fun List<GrunnlagDto>.hentSivilstandPerioder(
        periodeDto: VedtakPeriodeDto,
        gjelderPersonReferanse: Grunnlagsreferanse,
    ): List<GrunnlagDto> {
        val sivilstandPerioder =
            finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(Grunnlagstype.SIVILSTAND_PERIODE, periodeDto.grunnlagReferanseListe)
                .filter { it.gjelderReferanse == gjelderPersonReferanse }
        return sivilstandPerioder as List<GrunnlagDto>
    }

    private fun List<GrunnlagDto>.hentBarnIHusstandPerioder(periodeDto: VedtakPeriodeDto): List<GrunnlagDto> {
        val delberegningBarnIHusstand =
            finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(
                Grunnlagstype.DELBEREGNING_BARN_I_HUSSTAND,
                periodeDto.grunnlagReferanseListe,
            )

        val barnIHusstandPerioder =
            delberegningBarnIHusstand.flatMap {
                finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(Grunnlagstype.BOSTATUS_PERIODE, it.grunnlagsreferanseListe)
            }
        return barnIHusstandPerioder as List<GrunnlagDto>
    }

    private fun List<GrunnlagDto>.hentInntekter(
        periodeDto: VedtakPeriodeDto,
        gjelderPersonReferanse: Grunnlagsreferanse,
        gjelderBarnReferanse: Grunnlagsreferanse,
    ): List<GrunnlagDto> {
        val delberegningBidragsEvne =
            finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(
                Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL,
                periodeDto.grunnlagReferanseListe,
            ).firstOrNull() ?: return emptyList()

        val delberegningSumInntekt =
            finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(
                Grunnlagstype.DELBEREGNING_SUM_INNTEKT,
                delberegningBidragsEvne.grunnlagsreferanseListe,
            ).filter { it.gjelderReferanse == gjelderPersonReferanse }

        val inntekter =
            delberegningSumInntekt.flatMap {
                finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE, it.grunnlagsreferanseListe)
                    .filter { it.gjelderBarnReferanse == null || it.gjelderBarnReferanse == gjelderBarnReferanse }
                    .filter { it.gjelderReferanse == gjelderPersonReferanse }
            }
        return inntekter as List<GrunnlagDto>
    }

    private fun hentLøpendeForskuddForSak(
        saksnummer: String,
        søknadsbarnIdent: String,
    ): StønadDto? =
        bidragStønadConsumer.hentHistoriskeStønader(
            HentStønadHistoriskRequest(
                type = Stønadstype.FORSKUDD,
                sak = Saksnummer(saksnummer),
                skyldner = skyldnerNav,
                kravhaver = Personident(søknadsbarnIdent),
                gyldigTidspunkt = LocalDateTime.now(),
            ),
        )

//    fun byggGrunnlagForBeregning(): BeregnGrunnlag {
//        val personobjekter = tilPersonobjekter(søknadsbarnRolle)
//        val søknadsbarn = søknadsbarnRolle.tilGrunnlagPerson()
//        val bostatusBarn = tilGrunnlagBostatus(personobjekter)
//        val inntekter = tilGrunnlagInntekt(personobjekter, søknadsbarn, false)
//        val grunnlagsliste = (personobjekter + bostatusBarn + inntekter + byggGrunnlagSøknad()).toMutableSet()
//
//        return BeregnGrunnlag(
//            periode =
//                ÅrMånedsperiode(
//                    YearMonth.now(),
//                    YearMonth.now().plusMonths(1),
//                ),
//            stønadstype = Stønadstype.FORSKUDD,
//            søknadsbarnReferanse = søknadsbarn.referanse,
//            grunnlagListe = grunnlagsliste.toSet().toList(),
//        )
//    }
}
