package no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd

import no.nav.bidrag.automatiskjobb.consumer.BidragBehandlingConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import no.nav.bidrag.domene.enums.rolle.Rolletype
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class RevurderingslenkeRevurderingForskuddService(
    private val bidragSakConsumer: BidragSakConsumer,
    private val bidragBehandlingConsumer: BidragBehandlingConsumer,
    private val revurderForskuddRepository: RevurderForskuddRepository,
) {
    fun opprettRevurderingslenke(
        revurderingForskudd: RevurderingForskudd,
        søktFraDato: LocalDate,
    ): Int? {
        val sak = bidragSakConsumer.hentSak(revurderingForskudd.saksnummer)
        val bidragsmottaker = sak.roller.find { it.type == Rolletype.BIDRAGSMOTTAKER }
        val opprettBehandlingResponse =
            bidragBehandlingConsumer.opprettBehandlingForRevurderingAvForskudd(
                revurderingForskudd,
                bidragsmottaker?.fødselsnummer,
                sak.eierfogd,
                søktFraDato,
            )
        revurderingForskudd.oppgave = opprettBehandlingResponse.id
        revurderForskuddRepository.save(revurderingForskudd)
        return opprettBehandlingResponse.id
    }
}
