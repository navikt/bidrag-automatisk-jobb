package no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd

import no.nav.bidrag.automatiskjobb.consumer.BidragBehandlingConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import no.nav.bidrag.domene.enums.rolle.Rolletype
import org.springframework.stereotype.Service

@Service
class RevurderingslenkeRevurderingForskuddService(
    private val bidragSakConsumer: BidragSakConsumer,
    private val bidragBehandlingConsumer: BidragBehandlingConsumer,
    private val revurderForskuddRepository: RevurderForskuddRepository,
) {
    fun opprettRevurderingslenke(revurderingForskudd: RevurderingForskudd): Int? {
        val sak = bidragSakConsumer.hentSak(revurderingForskudd.barn.saksnummer)
        val bidragsmottaker = sak.roller.find { it.type == Rolletype.BIDRAGSMOTTAKER }
        val opprettBehandlingResponse =
            bidragBehandlingConsumer.opprettBehandlingForRevurderingAvForskudd(
                revurderingForskudd,
                bidragsmottaker?.fødselsnummer,
                sak.eierfogd,
            )
        revurderingForskudd.oppgave = opprettBehandlingResponse.id
        revurderForskuddRepository.save(revurderingForskudd)
        return opprettBehandlingResponse.id
    }
}
