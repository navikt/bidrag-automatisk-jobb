package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.revurderingslenke

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class RevurderingslenkeRevurderForskuddBatch(
    private val jobLauncher: JobLauncher,
    private val revurderingslenkeRevurderForskuddJob: Job,
) {
    fun start(søktFraDato: LocalDate?) {
        jobLauncher.run(
            revurderingslenkeRevurderForskuddJob,
            JobParametersBuilder()
                .addString("batchId", UUID.randomUUID().toString())
                .apply {
                    søktFraDato?.let { addString("soktFraDato", søktFraDato.toString()) }
                }.toJobParameters(),
        )
    }
}
