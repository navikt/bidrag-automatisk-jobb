package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.combinedLogger
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.mapper.VedtakMapper
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
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
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

data class AldersjusteringResultatResponse(
    val antall: Int,
    val saker: List<Saksnummer>,
    val resultatListe: List<AldersjusteringResultat>,
)

data class AldersjusteringResultat(
    val vedtaksid: Int,
    val stønadsid: Stønadsid,
    val vedtak: OpprettVedtakRequestDto,
)

@Service
class AldersjusteringService(
    private val barnRepository: BarnRepository,
    private val aldersjusteringOrchestrator: AldersjusteringOrchestrator,
    private val vedtakConsumer: BidragVedtakConsumer,
    private val sakConsumer: BidragSakConsumer,
    private val vedtakMapper: VedtakMapper,
) {
    fun kjørAldersjustering(
        år: Int,
        batchId: String,
    ) {
        val barnSomSkalAldersjusteres = hentAlleBarnSomSkalAldersjusteresForÅr(år)
        // TODO: Sjekk om barn finnes i aldersjustering tabell. Ellers lagre
        // TODO: Sjekk Status aldersjustering tabell. Ignorer hvis status ikke er UBEHANDLET
        barnSomSkalAldersjusteres.forEach { (alder, barnListe) ->
            secureLogger.info { "Aldersjustering av bidrag for aldersgruppe $alder" }
            barnListe.utførAldersjusteringBidrag(år, batchId)
        }
    }

    fun kjørAldersjusteringForSak(
        saksnummer: Saksnummer,
        år: Int,
        simuler: Boolean,
    ): AldersjusteringResultatResponse {
        val sak = sakConsumer.hentSak(saksnummer.verdi)
        val barnISaken = sak.roller.filter { it.type == Rolletype.BARN }
        val bp = sak.roller.find { it.type == Rolletype.BIDRAGSPLIKTIG } ?: ugyldigForespørsel("Fant ikke BP for sak $saksnummer")
        val resultat =
            barnISaken.mapNotNull {
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

        return AldersjusteringResultatResponse(
            resultat.size,
            resultat.map { it.stønadsid.sak },
            resultat,
        )
    }

    private fun List<Barn>.utførAldersjusteringBidrag(
        år: Int,
        batchId: String,
        simuler: Boolean = true,
    ) = forEach { utførAldersjusteringForBarn(Stønadstype.BIDRAG, it, år, batchId, simuler) }

    fun utførAldersjusteringForBarn(
        stønadstype: Stønadstype,
        barn: Barn,
        år: Int,
        batchId: String,
        simuler: Boolean = true,
    ): AldersjusteringResultat? {
        val stønadsid =
            Stønadsid(
                stønadstype,
                Personident(barn.kravhaver),
                Personident(barn.skyldner!!),
                Saksnummer(barn.saksnummer),
            )
        try {
            val resultatBeregning = aldersjusteringOrchestrator.utførAldersjustering(stønadsid, år)
            val vedtaksforslagRequest = vedtakMapper.tilOpprettVedtakRequest(resultatBeregning, stønadsid, batchId)
            slettEksisterendeVedtaksforslag(vedtaksforslagRequest.unikReferanse!!)
            if (simuler) {
                return AldersjusteringResultat(1, stønadsid, vedtaksforslagRequest)
            }

            val vedtaksid = vedtakConsumer.opprettVedtaksforslag(vedtaksforslagRequest)
            // TODO: Lagre vedtaksid
            // TODO: Status BEHANDLET
            // TODO: behandlingType = FATTET_FORSLAG
            return AldersjusteringResultat(vedtaksid, stønadsid, vedtaksforslagRequest)
        } catch (e: SkalIkkeAldersjusteresException) {
            // TODO: Håndter feil
            // TODO: Status BEHANDLET
            // TODO: behandlingType = INGEN
            combinedLogger.warn(e) { "Stønad $stønadsid skal ikke aldersjusteres" }
        } catch (e: AldersjusteresManueltException) {
            // TODO: Status BEHANDLET
            // TODO: behandlingType = MANUELL
            combinedLogger.warn(e) { "Stønad $stønadsid skal aldersjusteres manuelt" }
        } catch (e: Exception) {
            // TODO: Status BEHANDLET
            // TODO: behandlingType = FEILET
            combinedLogger.error(e) { "Det skjedde en feil ved aldersjustering for stønad $stønadsid" }
        }
        return null
    }

    private fun slettEksisterendeVedtaksforslag(referanse: String) {
        vedtakConsumer.hentVedtaksforslagBasertPåReferanase(referanse)?.let {
            log.info {
                "Fant eksisterende vedtaksforslag med referanse $referanse og id ${it.vedtaksid}. Sletter eksisterende vedtaksforslag "
            }
            vedtakConsumer.slettVedtaksforslag(it.vedtaksid.toInt())
        }
    }

    fun hentAlleBarnSomSkalAldersjusteresForÅr(år: Int): Map<Int, List<Barn>> =
        barnRepository
            .finnBarnSomSkalAldersjusteresForÅr(år)
            .groupBy { år - it.fødselsdato.year }
            .mapValues { it.value.sortedBy { barn -> barn.fødselsdato } }
}
