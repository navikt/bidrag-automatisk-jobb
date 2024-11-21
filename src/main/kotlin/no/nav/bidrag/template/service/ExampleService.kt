package no.nav.bidrag.template.service

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.template.consumer.BidragPersonConsumer
import no.nav.bidrag.template.model.HentPersonResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ExampleService(val bidragPersonConsumer: BidragPersonConsumer) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentDialogerForPerson(personident: Personident): HentPersonResponse {
        logger.info("Henter samtalereferat for person")
        return bidragPersonConsumer.hentPerson(personident)
    }
}
