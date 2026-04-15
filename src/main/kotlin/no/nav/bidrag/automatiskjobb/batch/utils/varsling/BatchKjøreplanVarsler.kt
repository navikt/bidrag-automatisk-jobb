package no.nav.bidrag.automatiskjobb.batch.utils.varsling

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.commons.service.slack.SlackService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOGGER = KotlinLogging.logger {}

@Component
class BatchKjøreplanVarsler(
    private val slackService: SlackService,
    private val batchKategorier: List<BatchKategori>,
) {
    private val dagFormat = DateTimeFormatter.ofPattern("d. MMMM", Locale.forLanguageTag("nb"))
    private val dagMedÅrFormat = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.forLanguageTag("nb"))
    private val månedÅrFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("nb"))

    @Scheduled(cron = $$"${SLACK_BATCH_KJOREPLAN_VARSLING_CRON:-}")
    @SchedulerLock(name = "batchKjoreplan", lockAtMostFor = "PT30M")
    fun sendBatchKjøreplanVarsel() = genererOgSendVarsel(LocalDate.now())

    internal fun genererOgSendVarsel(nå: LocalDate) {
        LOGGER.info { "Sender Slack-melding om planlagte batch-kjøringer denne måneden" }

        val header = ":calendar:  Planlagte automatiske batch-kjøringer i ${nå.format(månedÅrFormat)}"

        val kategorier =
            batchKategorier.joinToString("\n\n") { kategori ->
                val batchLinjer =
                    kategori.batcher.joinToString("\n") { oppføring ->
                        "    — ${oppføring.navn} - ${formaterKjøredato(oppføring.cron, nå)}"
                    }
                ":small_blue_diamond: ${kategori.navn}\n$batchLinjer"
            }

        slackService.sendMelding("$header\n\n$kategorier")
    }

    private fun formaterKjøredato(
        cron: String,
        nå: LocalDate,
    ): String {
        if (erDeaktivert(cron)) return "ikke satt opp til å kjøre automatisk"
        val split = cron.trim().split(" ")
        val dag = split[3].toInt()
        val måned = if (split.size > 4) split[4] else "*"

        return if (måned == "*") {
            formaterMånedligKjøring(dag, nå)
        } else {
            formaterÅrligKjøring(dag, måned.toInt(), nå)
        }
    }

    private fun formaterMånedligKjøring(
        dag: Int,
        nå: LocalDate,
    ): String {
        val kjøringDenneMåneden = if (dag <= nå.lengthOfMonth()) nå.withDayOfMonth(dag) else null
        return if (kjøringDenneMåneden != null && !kjøringDenneMåneden.isBefore(nå)) {
            // Kjøringen er denne måneden og har ikke passert
            kjøringDenneMåneden.format(dagFormat)
        } else {
            // Kjørdato har passert for inneværende måned
            val førsteDagNesteMåned = nå.plusMonths(1).withDayOfMonth(1)
            val nesteKjøring = førsteDagNesteMåned.withDayOfMonth(minOf(dag, førsteDagNesteMåned.lengthOfMonth()))
            // Kjørdato har passert for inneværende måned og neste måned er nytt år
            val nesteKjøringTekst =
                if (nesteKjøring.year != nå.year) nesteKjøring.format(dagMedÅrFormat) else nesteKjøring.format(dagFormat)
            "ingen kjøring inneværende måned. Neste kjøring er $nesteKjøringTekst"
        }
    }

    private fun formaterÅrligKjøring(
        dag: Int,
        måned: Int,
        nå: LocalDate,
    ): String {
        val dagIDenneMåneden = minOf(dag, YearMonth.of(nå.year, måned).lengthOfMonth())
        val kjøringDetteÅr = LocalDate.of(nå.year, måned, dagIDenneMåneden)

        return when {
            // Kjøringen er denne måneden og har ikke passert
            kjøringDetteÅr.month == nå.month && !kjøringDetteÅr.isBefore(nå) -> {
                kjøringDetteÅr.format(dagFormat)
            }

            // Kjøringen er ikke denne måneden og har ikke passert
            !kjøringDetteÅr.isBefore(nå) -> {
                "ingen kjøring inneværende måned. Neste kjøring er ${kjøringDetteÅr.format(dagFormat)}"
            }

            // Kjøringen har allerede vært i år, viser med årstall for neste kjøring
            else -> {
                val dagNesteÅr = minOf(dag, YearMonth.of(nå.year + 1, måned).lengthOfMonth())
                val nesteKjøring = LocalDate.of(nå.year + 1, måned, dagNesteÅr)
                "ingen kjøring inneværende måned. Neste kjøring er ${nesteKjøring.format(dagMedÅrFormat)}"
            }
        }
    }

    private fun erDeaktivert(cron: String): Boolean = cron.isBlank() || cron.trim() == "-"
}
