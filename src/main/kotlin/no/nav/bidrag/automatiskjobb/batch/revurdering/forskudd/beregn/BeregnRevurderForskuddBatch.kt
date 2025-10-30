package no.nav.bidrag.automatiskjobb.batch.revurdering.forskudd.beregn

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BeregnRevurderForskuddBatch(
    private val jobLauncher: JobLauncher,
    private val beregnRevurderForskuddJob: Job,
) {
    fun start(
        simuler: Boolean,
        antallMånederForBeregning: Long,
    ) {
        jobLauncher.run(
            beregnRevurderForskuddJob,
            JobParametersBuilder()
                .addString("simuler", simuler.toString())
                .addString("batchId", UUID.randomUUID().toString())
                .addString("antallManederForBeregning", antallMånederForBeregning.toString())
                .toJobParameters(),
        )
    }
}
