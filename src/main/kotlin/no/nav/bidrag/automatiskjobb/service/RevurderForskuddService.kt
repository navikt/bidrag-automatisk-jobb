package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.combinedLogger
import no.nav.bidrag.automatiskjobb.consumer.BidragStønadConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.mapper.GrunnlagMapper
import no.nav.bidrag.automatiskjobb.utils.erDirekteAvslag
import no.nav.bidrag.automatiskjobb.utils.tilResultatkode
import no.nav.bidrag.beregn.forskudd.BeregnForskuddApi
import no.nav.bidrag.domene.enums.beregning.Resultatkode.Companion.erDirekteAvslag
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.beregning.forskudd.ResultatBeregning
import no.nav.bidrag.transport.behandling.felles.grunnlag.søknadsbarn
import no.nav.bidrag.transport.behandling.stonad.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.stonad.response.StønadDto
import no.nav.bidrag.transport.behandling.stonad.response.StønadPeriodeDto
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.behandling.vedtak.saksnummer
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
    private val bidragVedtakConsumer: BidragVedtakConsumer,
    private val beregning: BeregnForskuddApi,
) {
    fun erForskuddRedusert(vedtakHendelse: VedtakHendelse): List<ForskuddRedusertResultat> {
        LOGGER.info { "Sjekker om forskuddet er redusert etter fattet vedtak ${vedtakHendelse.id} i sak ${vedtakHendelse.saksnummer}" }
        return erForskuddRedusertEtterFattetBidrag(vedtakHendelse) + erForskuddRedusertEtterSærbidrag(vedtakHendelse)
    }

    private fun erForskuddRedusertEtterSærbidrag(vedtakHendelse: VedtakHendelse): List<ForskuddRedusertResultat> {
        val vedtak = hentVedtak(vedtakHendelse.id) ?: return listOf()
        return vedtak.engangsbeløpListe
            .filter { it.type == Engangsbeløptype.SÆRBIDRAG }
            .mapNotNull {
                val gjelderBarn = it.kravhaver
                if (it.resultatkode.tilResultatkode()?.erDirekteAvslag() == true) {
                    LOGGER.info { "Særbidrag vedtaket er direkte avslag med resultat ${it.resultatkode} og har derfor ingen inntekter." }
                    return@mapNotNull null
                }
                val sistePeriode = hentLøpendeForskudd(it.sak.verdi, gjelderBarn.verdi) ?: return@mapNotNull null

                val vedtakForskudd = hentVedtak(sistePeriode.vedtaksid) ?: return@mapNotNull null
                val beregnetForskudd = beregnForskudd(vedtak, vedtakForskudd, gjelderBarn)

                val beløpLøpende = sistePeriode.beløp!!
                val erForskuddRedusert = beløpLøpende > beregnetForskudd.belop
                val sisteInntektForskudd = GrunnlagMapper.hentSisteDelberegningInntektFraForskudd(vedtakForskudd, it.kravhaver)
                val sisteInntektFattetVedtak = GrunnlagMapper.hentSisteDelberegningInntektFattetVedtak(vedtak, it.kravhaver)
                erForskuddRedusert.ifTrue { _ ->
                    combinedLogger.info {
                        """Forskudd er redusert i sak ${it.sak.verdi} for bidragsmottaker ${it.mottaker.verdi} og barn ${gjelderBarn.verdi}. 
                            Løpende forskudd er $beløpLøpende og forskudd ble beregnet til ${beregnetForskudd.belop} basert på siste vurdert inntekt.
                            Siste løpende inntekt for BM i fattet vedtak er ${sisteInntektFattetVedtak?.totalinntekt} og siste inntekt for BM i forskudd vedtaket er ${sisteInntektForskudd?.totalinntekt}
                        """.trimMargin()
                    }
                    ForskuddRedusertResultat(
                        saksnummer = it.sak.verdi,
                        bidragsmottaker = it.mottaker.verdi,
                        gjelderBarn = gjelderBarn.verdi,
                    )
                }
            }
    }

    private fun erForskuddRedusertEtterFattetBidrag(vedtakHendelse: VedtakHendelse): List<ForskuddRedusertResultat> {
        val vedtak = hentVedtak(vedtakHendelse.id) ?: return listOf()
        return vedtak.stønadsendringListe
            .filter { it.type == Stønadstype.BIDRAG }
            .mapNotNull {
                if (it.erDirekteAvslag()) {
                    LOGGER.info { "Bidrag vedtaket er direkte avslag og har derfor ingen inntekter." }
                    return@mapNotNull null
                }
                val gjelderBarn = it.kravhaver
                val sistePeriode = hentLøpendeForskudd(it.sak.verdi, gjelderBarn.verdi) ?: return@mapNotNull null

                val vedtakForskudd = hentVedtak(sistePeriode.vedtaksid) ?: return@mapNotNull null
                val beregnetForskudd = beregnForskudd(vedtak, vedtakForskudd, gjelderBarn)

                val beløpLøpende = sistePeriode.beløp!!
                val erForskuddRedusert = beløpLøpende > beregnetForskudd.belop
                val sisteInntektForskudd = GrunnlagMapper.hentSisteDelberegningInntektFraForskudd(vedtakForskudd, it.kravhaver)
                val sisteInntektFattetVedtak = GrunnlagMapper.hentSisteDelberegningInntektFattetVedtak(vedtak, it.kravhaver)
                erForskuddRedusert.ifTrue { _ ->
                    combinedLogger.info {
                        """Forskudd er redusert i sak ${it.sak.verdi} for bidragsmottaker ${it.mottaker.verdi} og barn ${gjelderBarn.verdi}. 
                            Løpende forskudd er $beløpLøpende og forskudd ble beregnet til ${beregnetForskudd.belop} basert på siste vurdert inntekt.
                            Siste løpende inntekt for BM i fattet vedtak er ${sisteInntektFattetVedtak?.totalinntekt} og siste inntekt for BM i forskudd vedtaket er ${sisteInntektForskudd?.totalinntekt}
                        """.trimMargin()
                    }
                    ForskuddRedusertResultat(
                        saksnummer = it.sak.verdi,
                        bidragsmottaker = it.mottaker.verdi,
                        gjelderBarn = gjelderBarn.verdi,
                    )
                }
            }
    }

    private fun hentVedtak(vedtakId: Int): VedtakDto? {
        val vedtak = bidragVedtakConsumer.hentVedtak(vedtakId) ?: return null
        if (vedtak.grunnlagListe.isEmpty()) {
            LOGGER.info { "Vedtak $vedtakId fattet av system ${vedtak.kildeapplikasjon} har mangler grunnlag. Gjør ingen vurdering" }
            return null
        }
        return vedtak
    }

    private fun beregnForskudd(
        vedtakBidrag: VedtakDto,
        vedtakLøpendeForskudd: VedtakDto,
        gjelderBarn: Personident,
    ): ResultatBeregning {
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
                    søknadsbarnReferanse = grunnlag.søknadsbarn.first().referanse,
                    grunnlagListe = grunnlag,
                ),
            )

        val sistePeriode = resultat.beregnetForskuddPeriodeListe.last()
        return sistePeriode.resultat
    }

    private fun hentLøpendeForskudd(
        saksnummer: String,
        gjelderBarn: String,
    ): StønadPeriodeDto? {
        val forskuddStønad =
            hentLøpendeForskuddForSak(saksnummer, gjelderBarn) ?: run {
                combinedLogger.info { "Fant ingen løpende forskudd i sak $saksnummer for barn $gjelderBarn" }
                return null
            }
        return forskuddStønad.periodeListe.hentSisteLøpendePeriode() ?: run {
            combinedLogger.info {
                "Forskudd i sak $saksnummer og barn $gjelderBarn har opphørt før dagens dato. Det finnes ingen løpende forskudd"
            }
            null
        }
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

    private fun List<StønadPeriodeDto>.hentSisteLøpendePeriode() =
        maxByOrNull { it.periode.fom }
            ?.takeIf { it.periode.til == null || it.periode.til!!.isAfter(YearMonth.now()) }
}
