package no.nav.bidrag.automatiskjobb.batch

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.Batch
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchKategori
import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchKjøreplanVarsler
import no.nav.bidrag.automatiskjobb.service.SlackService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@ExtendWith(MockKExtension::class)
class BatchKjøreplanVarslerTest {
    @MockK(relaxed = true)
    private lateinit var slackService: SlackService

    private val dagFormatter = DateTimeFormatter.ofPattern("d. MMMM", Locale.forLanguageTag("nb"))
    private val månedÅrFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("nb"))

    private fun lagScheduler(kategorier: List<BatchKategori>) = BatchKjøreplanVarsler(slackService, kategorier)

    private fun fangetMelding(): String {
        val slot = slot<String>()
        verify { slackService.sendMelding(capture(slot)) }
        return slot.captured
    }

    @Test
    fun `skal sende melding med korrekte datoer når alle batcher er aktive`() {
        val scheduler =
            lagScheduler(
                listOf(
                    BatchKategori(
                        navn = "Revurder forskudd",
                        batcher =
                            listOf(
                                Batch("Opprett", "0 0 0 5 * *"),
                                Batch("Evaluer", "0 0 0 6 * *"),
                                Batch("Fatte vedtak", "0 0 0 25 * *"),
                                Batch("Revurderingslenke", "0 0 0 25 * *"),
                            ),
                    ),
                ),
            )

        scheduler.sendBatchKjøreplanVarsel()

        val nå = LocalDate.now()
        val melding = fangetMelding()

        println(melding)
        melding shouldContain nå.format(månedÅrFormatter)
        melding shouldContain "Revurder forskudd"
        melding shouldContain "Opprett"
        melding shouldContain nå.withDayOfMonth(5).format(dagFormatter)
        melding shouldContain "Evaluer"
        melding shouldContain nå.withDayOfMonth(6).format(dagFormatter)
        melding shouldContain "Fatte vedtak"
        melding shouldContain "Revurderingslenke"
        melding shouldContain nå.withDayOfMonth(25).format(dagFormatter)
        melding shouldNotContain "ikke satt opp til å kjøre automatisk"
    }

    @Test
    fun `skal vise deaktivert-tekst for alle batcher når cron er satt til minus`() {
        val scheduler =
            lagScheduler(
                listOf(
                    BatchKategori(
                        navn = "Revurder forskudd",
                        batcher =
                            listOf(
                                Batch("Opprett", "-"),
                                Batch("Evaluer", "-"),
                                Batch("Fatte vedtak", "-"),
                                Batch("Revurderingslenke", "-"),
                            ),
                    ),
                ),
            )

        scheduler.sendBatchKjøreplanVarsel()

        val melding = fangetMelding()
        println(melding)

        melding shouldContain "Revurder forskudd"
        melding.split("ikke satt opp til å kjøre automatisk").size - 1 shouldBe 4
    }

    @Test
    fun `skal vise deaktivert-tekst for tom streng i cron`() {
        val scheduler =
            lagScheduler(
                listOf(
                    BatchKategori(
                        navn = "Revurder forskudd",
                        batcher = listOf(Batch("Opprett", "")),
                    ),
                ),
            )

        scheduler.sendBatchKjøreplanVarsel()

        val melding = fangetMelding()
        println(melding)
        melding shouldContain "ikke satt opp til å kjøre automatisk"
    }

    @Test
    fun `skal håndtere blanding av aktive og deaktiverte batcher`() {
        val scheduler =
            lagScheduler(
                listOf(
                    BatchKategori(
                        navn = "Revurder forskudd",
                        batcher =
                            listOf(
                                Batch("Opprett", "0 0 0 5 * *"),
                                Batch("Evaluer", "-"),
                                Batch("Fatte vedtak", "0 0 0 25 * *"),
                                Batch("Revurderingslenke", "-"),
                            ),
                    ),
                ),
            )

        scheduler.sendBatchKjøreplanVarsel()

        val nå = LocalDate.now()
        val melding = fangetMelding()
        println(melding)

        melding shouldContain nå.withDayOfMonth(5).format(dagFormatter)
        melding shouldContain nå.withDayOfMonth(25).format(dagFormatter)
        melding.split("ikke satt opp til å kjøre automatisk").size - 1 shouldBe 2
    }

    @Test
    fun `skal sende melding med alle kategorier når det finnes flere`() {
        val scheduler =
            lagScheduler(
                listOf(
                    BatchKategori(
                        navn = "Revurder forskudd",
                        batcher =
                            listOf(
                                Batch("Opprett", "0 0 0 5 * *"),
                                Batch("Evaluer", "0 0 0 6 * *"),
                            ),
                    ),
                    BatchKategori(
                        navn = "Aldersjustering",
                        batcher =
                            listOf(
                                Batch("Kjør", "0 0 0 10 * *"),
                            ),
                    ),
                    BatchKategori(
                        navn = "Forsendelse",
                        batcher =
                            listOf(
                                Batch("Send", "-"),
                            ),
                    ),
                ),
            )

        scheduler.sendBatchKjøreplanVarsel()

        val nå = LocalDate.now()
        val melding = fangetMelding()
        println(melding)

        melding shouldContain "Revurder forskudd"
        melding shouldContain nå.withDayOfMonth(5).format(dagFormatter)
        melding shouldContain nå.withDayOfMonth(6).format(dagFormatter)
        melding shouldContain "Aldersjustering"
        melding shouldContain nå.withDayOfMonth(10).format(dagFormatter)
        melding shouldContain "Forsendelse"
        melding shouldContain "ikke satt opp til å kjøre automatisk"
    }

    @Test
    fun `skal korrekt parse tosifret dag i cron-uttrykk`() {
        val scheduler =
            lagScheduler(
                listOf(
                    BatchKategori(
                        navn = "Revurder forskudd",
                        batcher = listOf(Batch("Fatte vedtak", "0 0 0 25 * *")),
                    ),
                ),
            )

        scheduler.sendBatchKjøreplanVarsel()

        val melding = fangetMelding()
        println(melding)

        melding shouldContain LocalDate.now().withDayOfMonth(25).format(dagFormatter)
    }

    @Test
    fun `skal sende melding selv når kategori-listen er tom`() {
        val scheduler = lagScheduler(emptyList())

        scheduler.sendBatchKjøreplanVarsel()

        val melding = fangetMelding()
        println(melding)

        melding shouldContain LocalDate.now().format(månedÅrFormatter)
        melding shouldNotContain "ikke satt opp til å kjøre automatisk"
    }
}
