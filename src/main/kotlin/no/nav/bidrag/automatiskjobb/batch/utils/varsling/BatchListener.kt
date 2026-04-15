package no.nav.bidrag.automatiskjobb.batch.utils.varsling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.service.slack.SlackMelding
import no.nav.bidrag.commons.service.slack.SlackService
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

private val LOGGER = KotlinLogging.logger {}

@Component
class BatchListener(
    private val slackService: SlackService,
) : JobExecutionListener {
    private val map = ConcurrentHashMap<Long, SlackMelding>()

    override fun beforeJob(jobExecution: JobExecution) {
        val jobNavn = jobExecution.jobInstance.jobName
        val slackMelding = slackService.sendMelding("Batch: $jobNavn startet!")
        map[jobExecution.id] = slackMelding
    }

    override fun afterJob(jobExecution: JobExecution) {
        val jobNavn = jobExecution.jobInstance.jobName
        val slackMelding = map.remove(jobExecution.id)

        if (jobExecution.status.isUnsuccessful) {
            LOGGER.error { "Batch: $jobNavn feilet!" }
            slackMelding?.oppdaterMelding("Batch: $jobNavn feilet! Se logg for detaljer.")
        } else if (jobExecution.status == BatchStatus.COMPLETED) {
            LOGGER.info { "Batch: $jobNavn fullført!" }
            slackMelding?.oppdaterMelding("Batch: $jobNavn fullført!")
        } else {
            LOGGER.warn { "Batch: $jobNavn avsluttet med status: ${jobExecution.status}" }
            slackMelding?.oppdaterMelding("Batch: $jobNavn avsluttet med status: ${jobExecution.status}")
        }

        // Rapporter skipped items per step
        jobExecution.stepExecutions.forEach { step ->
            val skipCount = step.skipCount
            if (skipCount > 0) {
                val melding =
                    "Batch: $jobNavn / step: ${step.stepName} hoppet over $skipCount item(s). " +
                        "Leste: ${step.readCount}, ${step.processSkipCount} skippet under prosessering, " +
                        "${step.writeSkipCount} skippet under skriving."
                LOGGER.warn { melding }
                slackMelding?.svarITråd(melding)
                step.failureExceptions.forEach { e ->
                    LOGGER.warn(e) { "  -> Årsak: ${e.message}" }
                }
            }
        }
    }
}
