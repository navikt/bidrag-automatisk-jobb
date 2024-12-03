package no.nav.bidrag.aldersjustering.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component

@Component
class AldersjusteringJobCompletionNotificationListener : JobExecutionListener {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(AldersjusteringJobCompletionNotificationListener::class.java)
    }

    override fun afterJob(jobExecution: JobExecution) {
        if (jobExecution.status.isUnsuccessful) {
            LOGGER.error("Aldersjusteringsbatch: ${jobExecution.jobInstance.jobName} feilet!")
        } else if (jobExecution.status == BatchStatus.COMPLETED) {
            LOGGER.info("Aldersjusteringsbatch: ${jobExecution.jobInstance.jobName} fullført!")
        } else {
            LOGGER.warn("Aldersjusteringsbatch: ${jobExecution.jobInstance.jobName} avsluttet med status: ${jobExecution.status}")
        }
    }
}
