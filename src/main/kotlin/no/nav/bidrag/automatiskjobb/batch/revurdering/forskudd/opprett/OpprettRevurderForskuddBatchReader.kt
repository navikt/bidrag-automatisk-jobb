package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.opprett

import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder
import org.springframework.stereotype.Component

@Component
class OpprettRevurderForskuddBatchReader(
    private val barnRepository: BarnRepository,
) : RepositoryItemReader<Barn>() {
    override fun read(): Barn? =
        RepositoryItemReaderBuilder<Barn>()
            .name("opprettRevurderForskuddBatchReader")
            .repository(barnRepository)
            .methodName("findBarnSomSkalRevurdereForskudd")
            .saveState(false) // Savestate må være false for å unngå feil ved parallell kjøring
            .build()
            .read()
}
