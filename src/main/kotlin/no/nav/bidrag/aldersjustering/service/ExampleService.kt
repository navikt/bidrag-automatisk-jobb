package no.nav.bidrag.aldersjustering.service

import no.nav.bidrag.aldersjustering.consumer.BidragPersonConsumer
import no.nav.bidrag.aldersjustering.model.HentPersonResponse
import no.nav.bidrag.domene.ident.Personident
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ExampleService(
    val bidragPersonConsumer: BidragPersonConsumer,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentDialogerForPerson(personident: Personident): HentPersonResponse {
        logger.info("Henter samtalereferat for person")
        return bidragPersonConsumer.hentPerson(personident)
    }
}
