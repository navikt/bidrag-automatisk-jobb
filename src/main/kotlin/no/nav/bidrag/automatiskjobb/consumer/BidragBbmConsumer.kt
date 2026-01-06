package no.nav.bidrag.automatiskjobb.consumer

import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningBBMConsumer
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningRequestDto
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningResponsDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Denne consumeren er kun opprettet for å blidgjøre vedtakService i bidrag-beregn-forskudd
 */
@Component
class BidragBbmConsumer(
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag-bbm"),
    BeregningBBMConsumer {
    override fun hentBeregning(request: BidragBeregningRequestDto): BidragBeregningResponsDto {
        TODO("Not yet implemented")
    }

    override fun hentAlleBeregninger(request: BidragBeregningRequestDto): BidragBeregningResponsDto {
        TODO("Not yet implemented")
    }
}
