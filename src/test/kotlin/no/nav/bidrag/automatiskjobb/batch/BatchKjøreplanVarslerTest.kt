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
import no.nav.bidrag.commons.service.slack.SlackService
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

    /** Referansedato i starten av måneden slik at dag 5, 6 og 25 alle er fremtidige */
    private val referanseDato = LocalDate.of(2026, 4, 1)

    private fun lagScheduler(kategorier: List<BatchKategori>) = BatchKjøreplanVarsler(slackService, kategorier)

    private fun kjørScheduler(
        kategorier: List<BatchKategori>,
        nå: LocalDate = referanseDato,
    ) = lagScheduler(kategorier).genererOgSendVarsel(nå)

    private fun fangetMelding(): String {
        val slot = slot<String>()
        verify { slackService.sendMelding(capture(slot)) }
        return slot.captured
    }

    @Test
    fun `skal sende melding med korrekte datoer når alle batcher er aktive`() {
        kjørScheduler(
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

        val melding = fangetMelding()

        println(melding)
        melding shouldContain referanseDato.format(månedÅrFormatter)
        melding shouldContain "Revurder forskudd"
        melding shouldContain "Opprett"
        melding shouldContain referanseDato.withDayOfMonth(5).format(dagFormatter)
        melding shouldContain "Evaluer"
        melding shouldContain referanseDato.withDayOfMonth(6).format(dagFormatter)
        melding shouldContain "Fatte vedtak"
        melding shouldContain "Revurderingslenke"
        melding shouldContain referanseDato.withDayOfMonth(25).format(dagFormatter)
        melding shouldNotContain "ikke satt opp til å kjøre automatisk"
    }

    @Test
    fun `skal vise deaktivert-tekst for alle batcher når cron er satt til minus`() {
        kjørScheduler(
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

        val melding = fangetMelding()
        println(melding)

        melding shouldContain "Revurder forskudd"
        melding.split("ikke satt opp til å kjøre automatisk").size - 1 shouldBe 4
    }

    @Test
    fun `skal vise deaktivert-tekst for tom streng i cron`() {
        kjørScheduler(
            listOf(
                BatchKategori(
                    navn = "Revurder forskudd",
                    batcher = listOf(Batch("Opprett", "")),
                ),
            ),
        )

        val melding = fangetMelding()
        println(melding)
        melding shouldContain "ikke satt opp til å kjøre automatisk"
    }

    @Test
    fun `skal håndtere blanding av aktive og deaktiverte batcher`() {
        kjørScheduler(
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

        val melding = fangetMelding()
        println(melding)

        melding shouldContain referanseDato.withDayOfMonth(5).format(dagFormatter)
        melding shouldContain referanseDato.withDayOfMonth(25).format(dagFormatter)
        melding.split("ikke satt opp til å kjøre automatisk").size - 1 shouldBe 2
    }

    @Test
    fun `skal sende melding med alle kategorier når det finnes flere`() {
        kjørScheduler(
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

        val melding = fangetMelding()
        println(melding)

        melding shouldContain "Revurder forskudd"
        melding shouldContain referanseDato.withDayOfMonth(5).format(dagFormatter)
        melding shouldContain referanseDato.withDayOfMonth(6).format(dagFormatter)
        melding shouldContain "Aldersjustering"
        melding shouldContain referanseDato.withDayOfMonth(10).format(dagFormatter)
        melding shouldContain "Forsendelse"
        melding shouldContain "ikke satt opp til å kjøre automatisk"
    }

    @Test
    fun `skal korrekt parse tosifret dag i cron-uttrykk`() {
        kjørScheduler(
            listOf(
                BatchKategori(
                    navn = "Revurder forskudd",
                    batcher = listOf(Batch("Fatte vedtak", "0 0 0 25 * *")),
                ),
            ),
        )

        val melding = fangetMelding()
        println(melding)

        melding shouldContain referanseDato.withDayOfMonth(25).format(dagFormatter)
    }

    @Test
    fun `skal sende melding selv når kategori-listen er tom`() {
        kjørScheduler(emptyList())

        val melding = fangetMelding()
        println(melding)

        melding shouldContain referanseDato.format(månedÅrFormatter)
        melding shouldNotContain "ikke satt opp til å kjøre automatisk"
    }

    @Test
    fun `skal vise ingen kjøring inneværende måned når måntlig kjøring allerede er passert`() {
        // referanseDato er 1. april – dag 10 er fremdeles fremtidig, men test med dag 31 (finnes ikke i april)
        val nå = LocalDate.of(2026, 4, 20)
        kjørScheduler(
            listOf(
                BatchKategori(
                    navn = "Batch",
                    batcher = listOf(Batch("Kjør", "0 0 0 10 * *")),
                ),
            ),
            nå = nå,
        )

        val melding = fangetMelding()
        println(melding)

        melding shouldContain "ingen kjøring inneværende måned"
        melding shouldContain "Neste kjøring er 10. mai"
    }

    @Test
    fun `skal vise neste kjøring med år når månedlig kjøring krysser årsskiftet`() {
        val nå = LocalDate.of(2026, 12, 20)
        kjørScheduler(
            listOf(
                BatchKategori(
                    navn = "Batch",
                    batcher = listOf(Batch("Kjør", "0 0 0 10 * *")),
                ),
            ),
            nå = nå,
        )

        val melding = fangetMelding()
        println(melding)

        melding shouldContain "ingen kjøring inneværende måned"
        melding shouldContain "10. januar 2027"
    }

    @Test
    fun `skal vise dato uten år for yearly cron som ennå ikke har kjørt i år`() {
        // Cron kjører 15. juni hvert år – referansedato er 1. april
        kjørScheduler(
            listOf(
                BatchKategori(
                    navn = "Batch",
                    batcher = listOf(Batch("Kjør", "0 0 0 15 6 *")),
                ),
            ),
        )

        val melding = fangetMelding()
        println(melding)

        melding shouldContain "ingen kjøring inneværende måned"
        melding shouldContain "Neste kjøring er 15. juni"
        melding shouldNotContain "2027"
    }

    @Test
    fun `skal vise dato med år for årlig jobb som allerede har kjørt i år`() {
        // Cron kjører 15. januar hvert år – referansedato er 1. april (januar er passert)
        kjørScheduler(
            listOf(
                BatchKategori(
                    navn = "Batch",
                    batcher = listOf(Batch("Kjør", "0 0 0 15 1 *")),
                ),
            ),
        )

        val melding = fangetMelding()
        println(melding)

        melding shouldContain "ingen kjøring inneværende måned"
        melding shouldContain "Neste kjøring er 15. januar 2027"
    }

    @Test
    fun `skal vise dato normalt for årlig kjøring som er planlagt i inneværende måned og ennå ikke er passert`() {
        // Cron kjører 15. april hvert år – referansedato er 1. april
        kjørScheduler(
            listOf(
                BatchKategori(
                    navn = "Batch",
                    batcher = listOf(Batch("Kjør", "0 0 0 15 4 *")),
                ),
            ),
        )

        val melding = fangetMelding()
        println(melding)

        melding shouldContain "15. april"
        melding shouldNotContain "ingen kjøring inneværende måned"
    }

    @Test
    fun `skal vise neste år for yearly cron der måneden er inneværende men dagen er passert`() {
        // Cron kjører 5. april hvert år – referansedato er 20. april (dagen er passert)
        kjørScheduler(
            listOf(
                BatchKategori(
                    navn = "Batch",
                    batcher = listOf(Batch("Kjør", "0 0 0 5 4 *")),
                ),
            ),
            nå = LocalDate.of(2026, 4, 20),
        )

        val melding = fangetMelding()
        println(melding)

        melding shouldContain "ingen kjøring inneværende måned"
        melding shouldContain "Neste kjøring er 5. april 2027"
    }
}
