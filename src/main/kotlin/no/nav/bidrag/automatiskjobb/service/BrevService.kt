package no.nav.bidrag.automatiskjobb.service

import no.nav.bidrag.automatiskjobb.consumer.BidragDokumentForsendelseConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.dokument.forsendelse.MottakerTo
import org.springframework.stereotype.Service

@Service
class BrevService(
    private val bidragDokumentForsendelseConsumer: BidragDokumentForsendelseConsumer,
    private val aldersjusteringRepository: AldersjusteringRepository,
    private val barnRepository: BarnRepository,
    private val bidragSakConsumer: BidragSakConsumer,
) {
    fun opprettForsendelse(aldersjustering: Aldersjustering) {
        val barn = barnRepository.findById(aldersjustering.barnId).orElseThrow { Exception("Barn not found") }
        val sak = bidragSakConsumer.hentSakerForPerson(Personident(barn.skyldner!!)) // TODO (Må vi legge til sak)

        val mottaker = MottakerTo() // TODO(For BM og BP opprett mottaker)

        val enhet = "" // TODO(Finne enhet)

        val forsendelse =
            bidragDokumentForsendelseConsumer.opprettForsendelse(
                aldersjustering,
                barn,
                mottaker,
                sak.fi,
            )
    }

    fun distribuerForsendelse(aldersjustering: Aldersjustering) {
    }
}
