package no.nav.bidrag.automatiskjobb.batch.utils.varsling

import no.nav.bidrag.commons.service.slack.SlackService
import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component

@Component
class BatchListener(
    private val slackService: SlackService
) : JobExecutionListener {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(BatchListener::class.java)
    }

    override fun beforeJob(jobExecution: JobExecution) {
        val jobNavn = jobExecution.jobInstance.jobName
        slackService.sendMelding("Batch: $jobNavn startet!")
    }

    override fun afterJob(jobExecution: JobExecution) {
        val jobNavn = jobExecution.jobInstance.jobName
        val threadTs = ""

        if (jobExecution.status.isUnsuccessful) {
            LOGGER.error("Batch: $jobNavn feilet!")
            slackService.sendMelding("Batch: $jobNavn feilet! Se logg for detaljer.")
        } else if (jobExecution.status == BatchStatus.COMPLETED) {
            LOGGER.info("Batch: $jobNavn fullført!")
            slackService.sendMelding("Batch: $jobNavn fullført!")
        } else {
            LOGGER.warn("Batch: $jobNavn avsluttet med status: ${jobExecution.status}")
            slackService.sendMelding("Batch: $jobNavn avsluttet med status: ${jobExecution.status}")
        }

        // Rapporter skipped items per step
        jobExecution.stepExecutions.forEach { step ->
            val skipCount = step.skipCount
            if (skipCount > 0) {
                LOGGER.warn(
                    "Batch: $jobNavn / step: ${step.stepName} hoppet over $skipCount item(s). " +
                        "Leste: ${step.readCount}, behandlet: ${step.processSkipCount} skippet under prosessering, " +
                        "${step.writeSkipCount} skippet under skriving.",
                )
                step.failureExceptions.forEach { e ->
                    LOGGER.warn("  -> Årsak: ${e.message}", e)
                }
            }
        }
    }
}
