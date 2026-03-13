package no.nav.bidrag.automatiskjobb.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component

@Component
class BatchCompletionNotificationListener : JobExecutionListener {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(BatchCompletionNotificationListener::class.java)
    }

    override fun afterJob(jobExecution: JobExecution) {
        val jobNavn = jobExecution.jobInstance.jobName

        if (jobExecution.status.isUnsuccessful) {
            LOGGER.error("Batch: $jobNavn feilet!")
        } else if (jobExecution.status == BatchStatus.COMPLETED) {
            LOGGER.info("Batch: $jobNavn fullført!")
        } else {
            LOGGER.warn("Batch: $jobNavn avsluttet med status: ${jobExecution.status}")
        }

        // Rapporter skipped items per steg
        jobExecution.stepExecutions.forEach { steg ->
            val skipCount = steg.skipCount
            if (skipCount > 0) {
                LOGGER.warn(
                    "Batch: $jobNavn / steg: ${steg.stepName} hoppet over $skipCount item(s). " +
                        "Leste: ${steg.readCount}, behandlet: ${steg.processSkipCount} skippet under prosessering, " +
                        "${steg.writeSkipCount} skippet under skriving.",
                )
                steg.failureExceptions.forEach { ex ->
                    LOGGER.warn("  -> Årsak: ${ex.message}", ex)
                }
            }
        }
    }
}
