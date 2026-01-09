package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.evaluer

import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class EvaluerRevurderForskuddBatchReaderClass {
    @Bean
    @StepScope
    fun evaluerRevurderForskuddBatchReader(
        revurderForskuddRepository: RevurderForskuddRepository,
        @Value("#{jobParameters['forMåned']}") forMånedString: String?,
    ): RepositoryItemReader<RevurderingForskudd> {
        val forMåned = forMånedString?.let { YearMonth.parse(it) } ?: YearMonth.now()

        return RepositoryItemReaderBuilder<RevurderingForskudd>()
            .name("evaluerRevurderForskuddBatchReader")
            .repository(revurderForskuddRepository)
            .methodName("findAllByForMåned")
            .arguments(listOf(forMåned))
            .pageSize(CHUNK_SIZE)
            .sorts(mapOf("id" to Sort.Direction.ASC))
            .saveState(false)
            .build()
    }
}
