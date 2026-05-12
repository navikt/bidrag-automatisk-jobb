package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.evaluer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.YearMonth
import java.util.UUID

private val LOGGER = KotlinLogging.logger { }

@Component
class EvaluerRevurderForskuddBatch(
    @param:Qualifier("asyncJobLauncher") private val jobLauncher: JobOperator,
    private val evaluerRevurderForskuddJob: Job,
) {
    fun start(
        simuler: Boolean,
        beregnFraMåned: YearMonth?,
        forMåned: YearMonth?,
        antallMånederForBeregning: Long = 3,
    ) {
        try {
            jobLauncher.start(
                evaluerRevurderForskuddJob,
                JobParametersBuilder()
                    .addString("simuler", simuler.toString())
                    .addString("batchId", UUID.randomUUID().toString())
                    .addString("antallManederForBeregning", antallMånederForBeregning.toString())
                    .apply {
                        beregnFraMåned?.let { addString("beregnFraManed", beregnFraMåned.toString()) }
                        forMåned?.let { addString("forManed", forMåned.toString()) }
                    }.toJobParameters(),
            )
        } catch (_: JobExecutionAlreadyRunningException) {
            LOGGER.warn { "Batch evaluerRevurderForskudd kjører allerede. Ignorerer ny forespørsel." }
        }
    }
}
