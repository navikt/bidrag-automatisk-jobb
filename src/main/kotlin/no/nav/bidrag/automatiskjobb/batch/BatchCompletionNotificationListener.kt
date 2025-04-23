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
        if (jobExecution.status.isUnsuccessful) {
            LOGGER.error("Batch: ${jobExecution.jobInstance.jobName} feilet!")
        } else if (jobExecution.status == BatchStatus.COMPLETED) {
            LOGGER.info("Batch: ${jobExecution.jobInstance.jobName} fullført!")
        } else {
            LOGGER.warn("Batch: ${jobExecution.jobInstance.jobName} avsluttet med status: ${jobExecution.status}")
        }
    }
}
