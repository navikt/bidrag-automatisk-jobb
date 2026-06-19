package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.rapporter

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.RapporterIndeksreguleringBidragData
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.RapporterIndeksreguleringBidragService
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import java.time.LocalDate
import java.time.Year

private val LOGGER = KotlinLogging.logger { }

/**
 * Tasklet som gjenskaper rapportstegene fra bisys `FB020Config`. Bygger rapportdata for gjennomførte
 * indeksreguleringer av bidrag for året (jobbparameter `aar`) og streamer de fem filene til
 * GCP-bucket i samme format som i bisys:
 *
 *  1. Bidragsreskontro (saker i Norge)
 *  2. FFU – BP i utlandet, brev bestilt
 *  3. FFU – BP i utlandet, diskresjon
 *  4. FFU – BP i utlandet, mangler adresse
 *  5. Elin – alle nye/indeksregulerte bidrag
 *
 * Bucket-mappe og filnavn settes via miljøvariabler. Mangler mappe/filnavn for en rapport, hoppes
 * den over (jf. `optionalStep` i bisys).
 */
class RapporterIndeksreguleringBidragTasklet(
    private val rapporterService: RapporterIndeksreguleringBidragService,
    private val filSkriver: RapportFilSkriver,
    private val mappe: String?,
    private val filnavnReskontro: String?,
    private val filnavnUtlandBrev: String?,
    private val filnavnUtlandDiskresjon: String?,
    private val filnavnUtlandManglerAdresse: String?,
    private val filnavnElin: String?,
) : Tasklet {
    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val år =
            chunkContext.stepContext.stepExecution.jobParameters
                .getString("aar")
                ?.toInt() ?: Year.now().value
        val indeksdato = LocalDate.now()
        val data = rapporterService.byggRapportData(år)

        val antallSkrevet = skrivAlleRapporter(data, indeksdato)
        contribution.incrementWriteCount(antallSkrevet.toLong())
        LOGGER.info { "Indeksregulering bidrag-rapporter ferdig for år $år. Lastet opp $antallSkrevet fil(er) til bucket." }
        return RepeatStatus.FINISHED
    }

    private fun skrivAlleRapporter(
        data: RapporterIndeksreguleringBidragData,
        indeksdato: LocalDate,
    ): Int {
        var skrevet = 0

        RapportFormatter.bidragsreskontro(data.bidragsreskontro, indeksdato)?.let { innhold ->
            if (filSkriver.skrivFil(mappe, filnavnReskontro, indeksdato, innhold)) skrevet++
        }
        RapportFormatter.bpUtland(data.bpUtlandBrev, BpUtlandRapportType.BREV_BESTILT, indeksdato)?.let { innhold ->
            if (filSkriver.skrivFil(mappe, filnavnUtlandBrev, indeksdato, innhold)) skrevet++
        }
        RapportFormatter.bpUtland(data.bpUtlandDiskresjon, BpUtlandRapportType.DISKRESJON, indeksdato)?.let { innhold ->
            if (filSkriver.skrivFil(mappe, filnavnUtlandDiskresjon, indeksdato, innhold)) skrevet++
        }
        RapportFormatter.bpUtland(data.bpUtlandManglerAdresse, BpUtlandRapportType.MANGLER_ADRESSE, indeksdato)?.let { innhold ->
            if (filSkriver.skrivFil(mappe, filnavnUtlandManglerAdresse, indeksdato, innhold)) skrevet++
        }
        RapportFormatter.elin(data.elin, indeksdato)?.let { innhold ->
            if (filSkriver.skrivFil(mappe, filnavnElin, indeksdato, innhold)) skrevet++
        }

        return skrevet
    }
}
