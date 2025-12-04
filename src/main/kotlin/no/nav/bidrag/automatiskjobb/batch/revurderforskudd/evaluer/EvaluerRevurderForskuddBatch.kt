package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.evaluer

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.time.YearMonth
import java.util.UUID

@Component
class EvaluerRevurderForskuddBatch(
    private val jobLauncher: JobLauncher,
    private val evaluerRevurderForskuddJob: Job,
) {
    fun start(
        simuler: Boolean,
        antallMånederForBeregning: Long,
        beregnFraMåned: YearMonth,
    ) {
        jobLauncher.run(
            evaluerRevurderForskuddJob,
            JobParametersBuilder()
                .addString("simuler", simuler.toString())
                .addString("batchId", UUID.randomUUID().toString())
                .addString("antallManederForBeregning", antallMånederForBeregning.toString())
                .addString("beregnFraManed", beregnFraMåned.toString())
                .toJobParameters(),
        )
    }
}
