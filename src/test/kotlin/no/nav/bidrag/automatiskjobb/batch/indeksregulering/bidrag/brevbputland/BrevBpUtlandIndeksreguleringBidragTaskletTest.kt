package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.brevbputland

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.IndeksreguleringsfilService
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.RapportLinje
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.RapporterIndeksreguleringBidragData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import java.math.BigDecimal
import java.time.Year

@ExtendWith(MockKExtension::class)
class BrevBpUtlandIndeksreguleringBidragTaskletTest {
    @MockK
    private lateinit var indeksreguleringsfilService: IndeksreguleringsfilService

    @InjectMockKs
    private lateinit var tasklet: BrevBpUtlandIndeksreguleringBidragTasklet

    private fun chunkContext(år: String?): ChunkContext {
        val chunkContext = mockk<ChunkContext>()
        every {
            chunkContext.stepContext.stepExecution.jobParameters
                .getString("aar")
        } returns år
        return chunkContext
    }

    private fun rapportLinje() =
        RapportLinje(
            saksnummer = "2600001",
            fnrBp = "11111111111",
            fnrBa = "22222222222",
            beløp = BigDecimal.valueOf(2000),
            landkode = "SE",
        )

    @Test
    fun `skal logge antall BP i utlandet og feile fordi brevoppretting ikke er implementert`() {
        every { indeksreguleringsfilService.byggRapportData(2026) } returns
            RapporterIndeksreguleringBidragData(bpUtlandBrev = listOf(rapportLinje(), rapportLinje()))

        shouldThrow<NotImplementedError> {
            tasklet.execute(mockk<StepContribution>(relaxed = true), chunkContext("2026"))
        }

        verify(exactly = 1) { indeksreguleringsfilService.byggRapportData(2026) }
    }

    @Test
    fun `skal bruke inneværende år når aar ikke er satt`() {
        every { indeksreguleringsfilService.byggRapportData(Year.now().value) } returns
            RapporterIndeksreguleringBidragData()

        shouldThrow<NotImplementedError> {
            tasklet.execute(mockk<StepContribution>(relaxed = true), chunkContext(null))
        }

        verify(exactly = 1) { indeksreguleringsfilService.byggRapportData(Year.now().value) }
    }
}
