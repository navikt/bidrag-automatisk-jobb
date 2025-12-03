package no.nav.bidrag.automatiskjobb.batch.oppdaterbarn

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OppdaterBarnBatch(
    private val jobLauncher: JobLauncher,
    private val oppdaterBarnJob: Job,
) {
    fun startOppdaterBarnBatch(
        barnId: String? = "",
        dagerTilbake: Int = 1,
        simuler: Boolean,
    ) {
        jobLauncher.run(
            oppdaterBarnJob,
            JobParametersBuilder()
                .addString("simuler", simuler.toString())
                .addString("barn", barnId ?: "")
                .addString("dager", dagerTilbake.toString())
                .addString("runId", UUID.randomUUID().toString())
                .toJobParameters(),
        )
    }
}
