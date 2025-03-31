package no.nav.bidrag.automatiskjobb.service

import no.nav.bidrag.automatiskjobb.combinedLogger
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.mapper.VedtakMapper
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.beregn.barnebidrag.service.AldersjusteresManueltException
import no.nav.bidrag.beregn.barnebidrag.service.AldersjusteringOrchestrator
import no.nav.bidrag.beregn.barnebidrag.service.SkalIkkeAldersjusteresException
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
import org.springframework.stereotype.Service

@Service
class AldersjusteringService(
    private val barnRepository: BarnRepository,
    private val aldersjusteringOrchestrator: AldersjusteringOrchestrator,
    private val vedtakConsumer: BidragVedtakConsumer,
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

    private fun List<Barn>.utførAldersjusteringBidrag(
        år: Int,
        batchId: String,
    ) = forEach { utførAldersjusteringForBarn(Stønadstype.BIDRAG, it, år, batchId) }

    fun utførAldersjusteringForBarn(
        stønadstype: Stønadstype,
        barn: Barn,
        år: Int,
        batchId: String,
    ) {
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
            val respons = vedtakConsumer.fatteVedtaksforslag(vedtaksforslagRequest)
            // TODO: Lagre vedtaksid
            // TODO: Status BEHANDLET
            // TODO: behandlingType = FATTET_FORSLAG
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
    }

    fun hentAlleBarnSomSkalAldersjusteresForÅr(år: Int): Map<Int, List<Barn>> =
        barnRepository
            .finnBarnSomSkalAldersjusteresForÅr(år)
            .groupBy { år - it.fødselsdato.year }
            .mapValues { it.value.sortedBy { barn -> barn.fødselsdato } }
}
