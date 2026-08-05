package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.gjennomfor

import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.springframework.batch.core.listener.StepExecutionListener
import org.springframework.batch.core.step.StepExecution
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder
import org.springframework.data.domain.Sort
import java.time.Year

class GjennomførIndeksreguleringBidragBatchReader(
    private val indeksreguleringRepository: IndeksreguleringRepository,
    private val pageSize: Int,
) : ItemReader<Indeksregulering>,
    StepExecutionListener {
    private var år: Int = Year.now().value

    private var delegate: RepositoryItemReader<Indeksregulering>? = null

    override fun beforeStep(stepExecution: StepExecution) {
        år = stepExecution.jobParameters.getString("aar")?.toInt() ?: Year.now().value
    }

    private fun delegate(): RepositoryItemReader<Indeksregulering> = delegate ?: byggDelegate().also { delegate = it }

    private fun byggDelegate(): RepositoryItemReader<Indeksregulering> =
        RepositoryItemReaderBuilder<Indeksregulering>()
            .name("gjennomforIndeksreguleringBidragBatchReader")
            .repository(indeksreguleringRepository)
            .saveState(false)
            .pageSize(pageSize)
            .sorts(mapOf("id" to Sort.Direction.ASC))
            .methodName("findAllByStønadstypeInAndÅr")
            .arguments(listOf(listOf(Stønadstype.BIDRAG, Stønadstype.BIDRAG18AAR, Stønadstype.OPPFOSTRINGSBIDRAG), år))
            .build()

    override fun read(): Indeksregulering? = delegate().read()
}
