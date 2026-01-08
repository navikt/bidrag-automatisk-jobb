package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.evaluer

import no.nav.bidrag.automatiskjobb.batch.BatchConfiguration.Companion.CHUNK_SIZE
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder
import org.springframework.context.annotation.Bean
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class EvaluerRevurderForskuddBatchReader {
    private var forMåned: YearMonth = YearMonth.now()

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        stepExecution.jobParameters.getString("forMåned")?.let { forMåned = YearMonth.parse(it) }
    }

    @Bean
    fun evaluerRevurderForskuddBatchReader(
        revurderForskuddRepository: RevurderForskuddRepository,
    ): RepositoryItemReader<RevurderingForskudd> =
        RepositoryItemReaderBuilder<RevurderingForskudd>()
            .name("evaluerRevurderForskuddBatchReader")
            .repository(revurderForskuddRepository)
            .methodName("findAllByForMåned")
            .arguments(listOf(forMåned))
            .pageSize(CHUNK_SIZE)
            .sorts(mapOf("id" to Sort.Direction.ASC))
            .saveState(false)
            .build()
}
