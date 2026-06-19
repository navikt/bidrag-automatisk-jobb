package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.rapporter

import no.nav.bidrag.automatiskjobb.batch.utils.varsling.BatchListener
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.RapporterIndeksreguleringBidragService
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class RapporterIndeksreguleringBidragBatchConfiguration {
    @Bean
    fun rapporterIndeksreguleringBidragJob(
        jobRepository: JobRepository,
        rapporterIndeksreguleringBidragStep: Step,
        listener: BatchListener,
    ): Job =
        JobBuilder("rapporterIndeksreguleringBidragJob", jobRepository)
            .listener(listener)
            .start(rapporterIndeksreguleringBidragStep)
            .build()

    @Bean
    fun rapporterIndeksreguleringBidragStep(
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager,
        rapporterIndeksreguleringBidragTasklet: RapporterIndeksreguleringBidragTasklet,
    ): Step =
        StepBuilder("rapporterIndeksreguleringBidragStep", jobRepository)
            .tasklet(rapporterIndeksreguleringBidragTasklet, transactionManager)
            .build()

    @Bean
    fun rapporterIndeksreguleringBidragTasklet(
        rapporterService: RapporterIndeksreguleringBidragService,
        filSkriver: RapportFilSkriver,
        @Value($$"${INDEKSREGULERING_BIDRAG_RAPPORT_MAPPE:-indeksregulering-bidrag/}") mappe: String,
        @Value($$"${INDEKSREGULERING_BIDRAG_RAPPORT_RESKONTRO_FILNAVN:-bidragsreskontro}") filnavnReskontro: String,
        @Value($$"${INDEKSREGULERING_BIDRAG_RAPPORT_UTLAND_BREV_FILNAVN:-bp-utland-brev}") filnavnUtlandBrev: String,
        @Value($$"${INDEKSREGULERING_BIDRAG_RAPPORT_UTLAND_DISKRESJON_FILNAVN:-bp-utland-diskresjon}") filnavnUtlandDiskresjon: String,
        @Value(
            $$"${INDEKSREGULERING_BIDRAG_RAPPORT_UTLAND_MANGLER_ADRESSE_FILNAVN:-bp-utland-mangler-adresse}",
        ) filnavnUtlandManglerAdresse: String,
        @Value($$"${INDEKSREGULERING_BIDRAG_RAPPORT_ELIN_FILNAVN:-elin}") filnavnElin: String,
    ): RapporterIndeksreguleringBidragTasklet =
        RapporterIndeksreguleringBidragTasklet(
            rapporterService = rapporterService,
            filSkriver = filSkriver,
            mappe = mappe,
            filnavnReskontro = filnavnReskontro,
            filnavnUtlandBrev = filnavnUtlandBrev,
            filnavnUtlandDiskresjon = filnavnUtlandDiskresjon,
            filnavnUtlandManglerAdresse = filnavnUtlandManglerAdresse,
            filnavnElin = filnavnElin,
        )
}
