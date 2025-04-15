package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.bidrag.automatiskjobb.combinedLogger
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.mapper.VedtakMapper
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringAldersjustertResultat
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringIkkeAldersjustertResultat
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringResponse
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringResultat
import no.nav.bidrag.automatiskjobb.service.model.AldersjusteringResultatResponse
import no.nav.bidrag.automatiskjobb.service.model.OpprettVedtakConflictResponse
import no.nav.bidrag.automatiskjobb.utils.ugyldigForespørsel
import no.nav.bidrag.beregn.barnebidrag.service.AldersjusteresManueltException
import no.nav.bidrag.beregn.barnebidrag.service.AldersjusteringOrchestrator
import no.nav.bidrag.beregn.barnebidrag.service.SkalIkkeAldersjusteresException
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException

private val log = KotlinLogging.logger {}

@Service
class AldersjusteringService(
    private val barnRepository: BarnRepository,
    private val aldersjusteringOrchestrator: AldersjusteringOrchestrator,
    private val vedtakConsumer: BidragVedtakConsumer,
    private val sakConsumer: BidragSakConsumer,
    private val vedtakMapper: VedtakMapper,
) {
    suspend fun kjørAldersjustering(
        år: Int,
        batchId: String,
        simuler: Boolean = true,
        pagesize: Pageable = Pageable.ofSize(100),
    ): AldersjusteringResponse {
        val barnSomSkalAldersjusteres = hentAlleBarnSomSkalAldersjusteresForÅr(år, pagesize = pagesize)
        // TODO: Sjekk om barn finnes i aldersjustering tabell. Ellers lagre
        // TODO: Sjekk Status aldersjustering tabell. Ignorer hvis status ikke er UBEHANDLET
        val resultat =
            barnSomSkalAldersjusteres
                .flatMap { (alder, barnListe) ->
                    secureLogger.info { "Aldersjustering av bidrag for aldersgruppe $alder. Simuler=$simuler" }
                    barnListe.utførAldersjusteringBidrag(år, batchId, simuler)
                }
        return AldersjusteringResponse(
            aldersjustert =
                resultat.filterIsInstance<AldersjusteringAldersjustertResultat>().let {
                    AldersjusteringResultatResponse(
                        antall = it.size,
                        stønadsider = it.map { barn -> barn.stønadsid },
                        detaljer = it,
                    )
                },
            ikkeAldersjustert =
                resultat.filterIsInstance<AldersjusteringIkkeAldersjustertResultat>().let {
                    AldersjusteringResultatResponse(
                        antall = it.size,
                        stønadsider = it.map { barn -> barn.stønadsid },
                        detaljer = it,
                    )
                },
        )
    }

    fun kjørAldersjusteringForSak(
        saksnummer: Saksnummer,
        år: Int,
        simuler: Boolean,
    ): AldersjusteringResponse {
        val sak = sakConsumer.hentSak(saksnummer.verdi)
        val barnISaken = sak.roller.filter { it.type == Rolletype.BARN }
        val bp = sak.roller.find { it.type == Rolletype.BIDRAGSPLIKTIG } ?: ugyldigForespørsel("Fant ikke BP for sak $saksnummer")
        val resultat =
            barnISaken.map {
                utførAldersjusteringForBarn(
                    Stønadstype.BIDRAG,
                    Barn(
                        kravhaver = it.fødselsnummer!!.verdi,
                        skyldner = bp.fødselsnummer!!.verdi,
                        saksnummer = saksnummer.verdi,
                    ),
                    år,
                    "aldersjustering-sak-$saksnummer",
                    simuler,
                )
            }

        return AldersjusteringResponse(
            aldersjustert =
                resultat.filterIsInstance<AldersjusteringAldersjustertResultat>().let {
                    AldersjusteringResultatResponse(
                        antall = it.size,
                        stønadsider = it.map { barn -> barn.stønadsid },
                        detaljer = it,
                    )
                },
            ikkeAldersjustert =
                resultat.filterIsInstance<AldersjusteringIkkeAldersjustertResultat>().let {
                    AldersjusteringResultatResponse(
                        antall = it.size,
                        stønadsider = it.map { barn -> barn.stønadsid },
                        detaljer = it,
                    )
                },
        )
    }

    private suspend fun List<Barn>.utførAldersjusteringBidrag(
        år: Int,
        batchId: String,
        simuler: Boolean = true,
    ): List<AldersjusteringResultat> =
        coroutineScope {
            map { barn ->
                async {
                    utførAldersjusteringForBarn(Stønadstype.BIDRAG, barn, år, batchId, simuler)
                }
            }.awaitAll()
        }

    fun utførAldersjusteringForBarn(
        stønadstype: Stønadstype,
        barn: Barn,
        år: Int,
        batchId: String,
        simuler: Boolean = true,
    ): AldersjusteringResultat {
        val stønadsid =
            Stønadsid(
                stønadstype,
                Personident(barn.kravhaver),
                Personident(barn.skyldner!!),
                Saksnummer(barn.saksnummer),
            )
        try {
            val (vedtaksidBeregning, resultatBeregning) = aldersjusteringOrchestrator.utførAldersjustering(stønadsid, år)
            val vedtaksforslagRequest = vedtakMapper.tilOpprettVedtakRequest(resultatBeregning, stønadsid, batchId)
            if (simuler) {
                log.info { "Kjører aldersjustering i simuleringsmodus. Oppretter ikke vedtaksforslag" }
                return AldersjusteringAldersjustertResultat(-1, stønadsid, vedtaksforslagRequest)
            }

            val vedtaksid = opprettEllerOppdaterVedtaksforslag(vedtaksforslagRequest)
            // TODO: Lagre vedtaksid
            // TODO: Status BEHANDLET
            // TODO: behandlingType = FATTET_FORSLAG
            return AldersjusteringAldersjustertResultat(vedtaksid, stønadsid, vedtaksforslagRequest)
        } catch (e: SkalIkkeAldersjusteresException) {
            // TODO: Håndter feil
            // TODO: Status BEHANDLET
            // TODO: behandlingType = INGEN
            combinedLogger.warn(e) { "Stønad $stønadsid skal ikke aldersjusteres med begrunnelse ${e.begrunnelser.joinToString(", ")}" }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, e.begrunnelser.joinToString(", "))
        } catch (e: AldersjusteresManueltException) {
            // TODO: Status BEHANDLET
            // TODO: behandlingType = MANUELL
            combinedLogger.warn(e) { "Stønad $stønadsid skal aldersjusteres manuelt med begrunnelse ${e.begrunnelse}" }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, e.begrunnelse.name)
        } catch (e: Exception) {
            // TODO: Status BEHANDLET
            // TODO: behandlingType = FEILET
            combinedLogger.error(e) { "Det skjedde en feil ved aldersjustering for stønad $stønadsid" }
            return AldersjusteringIkkeAldersjustertResultat(stønadsid, "Teknisk feil: ${e.message}")
        }
    }

    private fun opprettEllerOppdaterVedtaksforslag(request: OpprettVedtakRequestDto) =
        try {
            slettEksisterendeVedtaksforslag(request.unikReferanse!!)
            vedtakConsumer.opprettVedtaksforslag(request)
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                log.info { "Vedtaksforslag med referanse ${request.unikReferanse} finnes allerede. Oppdaterer vedtaksforslaget" }
                val resultat = e.getResponseBodyAs(OpprettVedtakConflictResponse::class.java)!!
                vedtakConsumer.oppdaterVedtaksforslag(resultat.vedtaksid, request)
            } else {
                log.error(e) { "Feil ved oppretting av vedtaksforslag med referanse ${request.unikReferanse}" }
                throw e
            }
        }

    private fun slettEksisterendeVedtaksforslag(referanse: String) {
        vedtakConsumer.hentVedtaksforslagBasertPåReferanase(referanse)?.let {
            log.info {
                "Fant eksisterende vedtaksforslag med referanse $referanse og id ${it.vedtaksid}. Sletter eksisterende vedtaksforslag "
            }
            vedtakConsumer.slettVedtaksforslag(it.vedtaksid.toInt())
        }
    }

    fun hentAlleBarnSomSkalAldersjusteresForÅr(
        år: Int,
        pagesize: Pageable = Pageable.ofSize(100),
    ): Map<Int, List<Barn>> =
        barnRepository
            .finnBarnSomSkalAldersjusteresForÅr(år, pageable = pagesize)
            .groupBy { år - it.fødselsdato.year }
            .mapValues { it.value.sortedBy { barn -> barn.fødselsdato } }
}
