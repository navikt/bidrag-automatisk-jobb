package no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd

import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import no.nav.bidrag.automatiskjobb.service.OppgaveService
import org.springframework.stereotype.Service

@Service
class OppgaveRevurderingForskuddService(
    private val oppgaveService: OppgaveService,
    private val revurderForskuddRepository: RevurderForskuddRepository,
) {
    fun opprettOppgave(revurderingForskudd: RevurderingForskudd): Int {
        val oppgaveId = oppgaveService.opprettOppgaveForTilbakekrevingAvForskudd(revurderingForskudd)

        revurderingForskudd.oppgave = oppgaveId
        revurderForskuddRepository.save(revurderingForskudd)
        return oppgaveId
    }
}
