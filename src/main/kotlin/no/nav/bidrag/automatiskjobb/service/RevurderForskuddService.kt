package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.combinedLogger
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragStønadConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.mapper.GrunnlagMapper
import no.nav.bidrag.automatiskjobb.mapper.erBidrag
import no.nav.bidrag.automatiskjobb.utils.enesteResultatkode
import no.nav.bidrag.automatiskjobb.utils.erDirekteAvslag
import no.nav.bidrag.automatiskjobb.utils.tilResultatkode
import no.nav.bidrag.beregn.barnebidrag.service.SisteManuelleVedtak
import no.nav.bidrag.beregn.forskudd.BeregnForskuddApi
import no.nav.bidrag.beregn.vedtak.Vedtaksfiltrering
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.beregning.Resultatkode.Companion.erDirekteAvslag
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.felles.personidentNav
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.beregning.forskudd.ResultatPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.personIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.søknadsbarn
import no.nav.bidrag.transport.behandling.stonad.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.stonad.response.StønadDto
import no.nav.bidrag.transport.behandling.stonad.response.StønadPeriodeDto
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.behandling.vedtak.request.HentVedtakForStønadRequest
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
    val stønadstype: Stønadstype? = null,
    val engangsbeløptype: Engangsbeløptype? = null,
)

data class StønadEngangsbeløpId(
    val kravhaver: Personident,
    val skyldner: Personident,
    val sak: Saksnummer,
    val engangsbeløptype: Engangsbeløptype? = null,
    val stønadstype: Stønadstype? = null,
)

private val LOGGER = KotlinLogging.logger {}

private fun VedtakDto.erIndeksreguleringEllerAldersjustering() =
    listOf(Vedtakstype.ALDERSJUSTERING, Vedtakstype.INDEKSREGULERING).contains(type)

@Service
@Import(BeregnForskuddApi::class, Vedtaksfiltrering::class)
class RevurderForskuddService(
    private val bidragStønadConsumer: BidragStønadConsumer,
    private val bidragVedtakConsumer: BidragVedtakConsumer,
    private val bidragSakConsumer: BidragSakConsumer,
    private val beregning: BeregnForskuddApi,
    private val vedtaksFilter: Vedtaksfiltrering,
) {
    fun erForskuddRedusert(vedtakHendelse: VedtakHendelse): List<ForskuddRedusertResultat> {
        combinedLogger.info {
            "Sjekker om forskuddet er redusert etter fattet vedtak ${vedtakHendelse.id} i sak ${vedtakHendelse.saksnummer}"
        }
        val vedtak = hentVedtak(vedtakHendelse.id) ?: return listOf()
        return erForskuddRedusertEtterFattetBidrag(SisteManuelleVedtak(vedtakHendelse.id, vedtak)) +
            erForskuddRedusertEtterSærbidrag(SisteManuelleVedtak(vedtakHendelse.id, vedtak))
    }

    private fun erForskuddRedusertEtterSærbidrag(vedtakInfo: SisteManuelleVedtak): List<ForskuddRedusertResultat> {
        val (vedtaksid, vedtak) = vedtakInfo
        return vedtak.engangsbeløpListe
            .filter { it.type == Engangsbeløptype.SÆRBIDRAG }
            .filter {
                if (it.resultatkode.tilResultatkode()?.erDirekteAvslag() == true) {
                    combinedLogger.info {
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
                    val stønadsid = StønadEngangsbeløpId(rolle.fødselsnummer!!, stønad.skyldner, stønad.sak, engangsbeløptype = stønad.type)
                    erForskuddetRedusert(vedtakInfo, stønadsid, stønad.mottaker)
                }
            }
    }

    private fun erForskuddRedusertEtterFattetBidrag(vedtakInfo: SisteManuelleVedtak): List<ForskuddRedusertResultat> =
        vedtakInfo.vedtak.stønadsendringListe
            .filter { it.erBidrag }
            .filter {
                if (it.erDirekteAvslag()) {
                    combinedLogger.info {
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
                    val stønadsid = StønadEngangsbeløpId(rolle.fødselsnummer!!, stønad.skyldner, stønad.sak, stønadstype = stønad.type)
                    erForskuddetRedusert(vedtakInfo, stønadsid, bidragsmottaker.fødselsnummer!!)
                }
            }

    fun erForskuddetRedusert(
        vedtakFattet: SisteManuelleVedtak,
        stønadEngangsbeløpId: StønadEngangsbeløpId,
        mottaker: Personident,
    ): ForskuddRedusertResultat? {
        val gjelderBarn = stønadEngangsbeløpId.kravhaver
        val sistePeriode = hentLøpendeForskudd(stønadEngangsbeløpId.sak.verdi, gjelderBarn.verdi) ?: return null

        val vedtakForskudd = hentSisteManuelleForskuddVedtak(sistePeriode.vedtaksid, stønadEngangsbeløpId.sak, gjelderBarn) ?: return null
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
            secureLogger.info {
                """Forskudd er redusert i sak ${stønadEngangsbeløpId.sak.verdi} for bidragsmottaker ${mottaker.verdi} og barn ${gjelderBarn.verdi}. 
                   Løpende forskudd er $beløpLøpende og forskudd ble beregnet til ${beregnetResultat.belop} basert på siste vurdert inntekt fra bidrag vedtak ${vedtakFattet.vedtaksId} og grunnlag fra forskudd vedtak ${vedtakForskudd.vedtaksId}.
                   Siste løpende inntekt for BM i fattet vedtak er ${sisteInntektFattetVedtak?.totalinntekt} og siste inntekt for BM i forskudd vedtaket er ${sisteInntektForskudd?.totalinntekt}.
                   
                   Innteksdetaljer fra fattet vedtak $sisteInntektFattetVedtak 
                   og fra forskudd vedtaket $sisteInntektForskudd
                """.trimMargin()
                    .trimIndent()
            }
            ForskuddRedusertResultat(
                saksnummer = stønadEngangsbeløpId.sak.verdi,
                bidragsmottaker = mottaker.verdi,
                gjelderBarn = gjelderBarn.verdi,
                stønadstype = stønadEngangsbeløpId.stønadstype,
                engangsbeløptype = stønadEngangsbeløpId.engangsbeløptype,
            )
        } ?: run {
            secureLogger.info {
                """Forskudd er IKKE redusert i sak ${stønadEngangsbeløpId.sak.verdi} for bidragsmottaker ${mottaker.verdi} og barn ${gjelderBarn.verdi}. 
                   Løpende forskudd er $beløpLøpende og forskudd ble beregnet til ${beregnetResultat.belop} basert på siste vurdert inntekt fra bidrag vedtak ${vedtakFattet.vedtaksId} og grunnlag fra forskudd vedtak ${vedtakForskudd.vedtaksId}.
                   Siste løpende inntekt for BM i fattet vedtak er $sisteInntektFattetVedtak og siste inntekt for BM i forskudd vedtaket er $sisteInntektForskudd.
                   
                   Innteksdetaljer fra fattet vedtak $sisteInntektFattetVedtak 
                   og fra forskudd vedtaket $sisteInntektForskudd
                """.trimMargin()
                    .trimIndent()
            }
            null
        }
    }

    private fun hentVedtak(vedtakId: Int): VedtakDto? {
        val vedtak = bidragVedtakConsumer.hentVedtak(vedtakId) ?: return null
        if (vedtak.grunnlagListe.isEmpty()) {
            combinedLogger.info {
                "Vedtak $vedtakId fattet av system ${vedtak.kildeapplikasjon} mangler grunnlag. Gjør ingen vurdering"
            }
            return null
        }
        return vedtak
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
                    bidragVedtakConsumer.hentVedtak(sisteManuelleVedtak.vedtaksid.toInt())?.let {
                        SisteManuelleVedtak(sisteManuelleVedtak.vedtaksid.toInt(), it)
                    }
                } else {
                    SisteManuelleVedtak(vedtakId, it)
                }
            } ?: return null

        if (vedtak.vedtak.grunnlagListe.isEmpty()) {
            combinedLogger.info {
                "Forskudd vedtak $vedtakId fattet av system ${vedtak.vedtak.kildeapplikasjon} mangler grunnlag. Gjør ingen vurdering"
            }
            return null
        }
        secureLogger.info { "Fant siste manuelle forskudd vedtak ${vedtak.vedtaksId}" }
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
                skyldner = personidentNav,
                kravhaver = Personident(søknadsbarnIdent),
                gyldigTidspunkt = LocalDateTime.now(),
            ),
        )

    private fun List<StønadPeriodeDto>.hentSisteLøpendePeriode() =
        maxByOrNull { it.periode.fom }
            ?.takeIf { it.periode.til == null || it.periode.til!!.isAfter(YearMonth.now()) }
}
