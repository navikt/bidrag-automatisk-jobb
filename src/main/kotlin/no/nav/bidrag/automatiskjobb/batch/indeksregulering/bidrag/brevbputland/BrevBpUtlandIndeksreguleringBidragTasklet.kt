package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.brevbputland

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.IndeksreguleringsfilService
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import java.time.Year

private val LOGGER = KotlinLogging.logger { }

/**
 * Tasklet som skal opprette brev til BP i utlandet for fattede indeksreguleringer av bidrag.
 *
 * Brevoppretting er ikke implementert enda. Tasklet-en finner BP-er med adresse utenfor Norge
 * (samme klassifisering som rapporter-batchen), logger hvor mange som skulle hatt brev og
 * feiler deretter batchen.
 */
class BrevBpUtlandIndeksreguleringBidragTasklet(
    private val indeksreguleringsfilService: IndeksreguleringsfilService,
) : Tasklet {
    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val år =
            chunkContext.stepContext.stepExecution.jobParameters
                .getString("aar")
                ?.toInt() ?: Year.now().value

        val bpUtlandBrev = indeksreguleringsfilService.byggRapportData(år).bpUtlandBrev

        LOGGER.error {
            "Det skulle vært sendt brev til ${bpUtlandBrev.size} BP-er i utlandet for fattede indeksreguleringer " +
                "av bidrag for år $år, men det er ikke opprettet noen brev."
        }

        // TODO: Implementer oppretting av brev til BP i utlandet for indeksregulering av bidrag.
        throw NotImplementedError(
            "Oppretting av brev til BP i utlandet er ikke implementert enda. " +
                "${bpUtlandBrev.size} BP-er skulle hatt brev for indeksregulering år $år.",
        )
    }
}
