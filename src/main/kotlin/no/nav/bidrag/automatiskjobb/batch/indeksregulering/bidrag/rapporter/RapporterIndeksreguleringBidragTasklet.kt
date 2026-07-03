package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.rapporter

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.rapporter.RapporterIndeksreguleringBidragTasklet.Companion.MAPPE
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.IndeksreguleringsfilService
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.RapporterIndeksreguleringBidragData
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import java.time.LocalDate
import java.time.Year

private val LOGGER = KotlinLogging.logger { }

/**
 * Tasklet som bygger rapportdata for gjennomførte
 * indeksreguleringer av bidrag for året og streamer de fem filene til GCP-bucket
 *
 *  1. Bidragsreskontro - saker i Norge
 *  2. BP i utlandet
 *  3. BP i utlandet med diskresjon
 *  4. BP i utlandet som mangler adresse
 *  5. Elin – alle indeksregulerte bidrag
 *
 * Bucket-mappe og filnavn er faste verdier definert i companion object, hver filtype
 * skrives til sin egen undermappe under [MAPPE].
 */
class RapporterIndeksreguleringBidragTasklet(
    private val indeksreguleringsfilService: IndeksreguleringsfilService,
) : Tasklet {
    companion object {
        const val MAPPE = "indeksregulering-bidrag/"

        const val SUBMAPPE_RESKONTRO = "bidragsreskontro/"
        const val SUBMAPPE_UTLAND_BREV = "bp-utland-brev/"
        const val SUBMAPPE_UTLAND_DISKRESJON = "bp-utland-diskresjon/"
        const val SUBMAPPE_UTLAND_MANGLER_ADRESSE = "bp-utland-mangler-adresse/"
        const val SUBMAPPE_ELIN = "elin/"

        const val FILNAVN_RESKONTRO = "bidragsreskontro"
        const val FILNAVN_UTLAND_BREV = "bp-utland-brev"
        const val FILNAVN_UTLAND_DISKRESJON = "bp-utland-diskresjon"
        const val FILNAVN_UTLAND_MANGLER_ADRESSE = "bp-utland-mangler-adresse"
        const val FILNAVN_ELIN = "elin"
    }

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val år =
            chunkContext.stepContext.stepExecution.jobParameters
                .getString("aar")
                ?.toInt() ?: Year.now().value
        val indeksdato = LocalDate.now()
        val data = indeksreguleringsfilService.byggRapportData(år)

        val antallSkrevet = skrivFiler(data, indeksdato)
        contribution.incrementWriteCount(antallSkrevet.toLong())
        LOGGER.info { "Indeksregulering bidrag-rapporter ferdig for år $år. Lastet opp $antallSkrevet fil(er) til bucket." }
        return RepeatStatus.FINISHED
    }

    private fun skrivFiler(
        data: RapporterIndeksreguleringBidragData,
        indeksdato: LocalDate,
    ): Int {
        var skrevet = 0

        Filformaterer.bidragsreskontro(data.bidragsreskontro, indeksdato)?.let { innhold ->
            if (indeksreguleringsfilService.lastOppFil(MAPPE + SUBMAPPE_RESKONTRO, FILNAVN_RESKONTRO, indeksdato, innhold)) skrevet++
        }
        Filformaterer.bpUtland(data.bpUtlandBrev, BpUtlandRapportType.BREV_BESTILT, indeksdato)?.let { innhold ->
            if (indeksreguleringsfilService.lastOppFil(MAPPE + SUBMAPPE_UTLAND_BREV, FILNAVN_UTLAND_BREV, indeksdato, innhold)) skrevet++
        }
        Filformaterer.bpUtland(data.bpUtlandDiskresjon, BpUtlandRapportType.DISKRESJON, indeksdato)?.let { innhold ->
            if (indeksreguleringsfilService.lastOppFil(
                    MAPPE + SUBMAPPE_UTLAND_DISKRESJON,
                    FILNAVN_UTLAND_DISKRESJON,
                    indeksdato,
                    innhold,
                )
            ) {
                skrevet++
            }
        }
        Filformaterer.bpUtland(data.bpUtlandManglerAdresse, BpUtlandRapportType.MANGLER_ADRESSE, indeksdato)?.let { innhold ->
            if (indeksreguleringsfilService.lastOppFil(
                    MAPPE + SUBMAPPE_UTLAND_MANGLER_ADRESSE,
                    FILNAVN_UTLAND_MANGLER_ADRESSE,
                    indeksdato,
                    innhold,
                )
            ) {
                skrevet++
            }
        }
        Filformaterer.elin(data.elin, indeksdato)?.let { innhold ->
            if (indeksreguleringsfilService.lastOppFil(MAPPE + SUBMAPPE_ELIN, FILNAVN_ELIN, indeksdato, innhold)) {
                skrevet++
                indeksreguleringsfilService.lastOppFilTilFilsluse(MAPPE + SUBMAPPE_ELIN, FILNAVN_ELIN, indeksdato)
            }
        }

        return skrevet
    }
}
