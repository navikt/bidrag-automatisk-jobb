package no.nav.bidrag.automatiskjobb.batch.utils.varsling

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.automatiskjobb.service.SlackService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOGGER = KotlinLogging.logger {}

@Component
class BatchKjøreplanVarsler(
    private val slackService: SlackService,
    private val batchKategorier: List<BatchKategori>,
) {
    private val dagFormat = DateTimeFormatter.ofPattern("d. MMMM", Locale.forLanguageTag("nb"))
    private val månedÅrFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("nb"))

    @Scheduled(cron = $$"${SLACK_BATCH_KJOREPLAN_VARSLING_CRON:-}")
    @SchedulerLock(name = "batchKjoreplan", lockAtMostFor = "PT30M")
    fun sendBatchKjøreplanVarsel() {
        LOGGER.info { "Sender Slack-melding om planlagte batch-kjøringer denne måneden" }
        val nå = LocalDate.now()

        val header = "* Planlagte automatiske batch-kjøringer i ${nå.format(månedÅrFormat)} *"

        val kategorier =
            batchKategorier.joinToString("\n\n") { kategori ->
                val batchLinjer =
                    kategori.batcher.joinToString("\n") { oppføring ->
                        "    ∙ ${oppføring.navn} - ${formaterKjøredato(oppføring.cron, nå)}"
                    }
                "* ${kategori.navn} *\n$batchLinjer"
            }

        slackService.sendMelding("$header\n\n$kategorier")
    }

    private fun formaterKjøredato(
        cron: String,
        nå: LocalDate,
    ): String {
        if (erDeaktivert(cron)) return "ikke satt opp til å kjøre automatisk"
        val dag = cron.trim().split(" ")[3].toInt()
        return nå.withDayOfMonth(dag).format(dagFormat)
    }

    private fun erDeaktivert(cron: String): Boolean = cron.isBlank() || cron.trim() == "-"
}
