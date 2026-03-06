package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.revurderingslenke

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd.RevurderingslenkeRevurderingForskuddService
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth

private val LOGGER = KotlinLogging.logger { }

@Component
class RevurderingslenkeRevurderForskuddBatchProcessor(
    private val revurderingslenkeRevurderingForskuddService: RevurderingslenkeRevurderingForskuddService,
) : ItemProcessor<RevurderingForskudd, Int?> {
    private var søktFraDato = LocalDate.now().minusMonths(12).withDayOfMonth(1)

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        stepExecution.jobParameters.getString("soktFraDato")?.let { søktFraDato = LocalDate.parse(it) }
    }

    override fun process(revurderingForskudd: RevurderingForskudd): Int? =
        try {
            revurderingslenkeRevurderingForskuddService.opprettRevurderingslenke(revurderingForskudd, søktFraDato)
        } catch (e: Exception) {
            LOGGER.error(e) { "Det skjedde en feil ved opprettelse av oppgave for aldersjustering ${revurderingForskudd.id}" }
            null
        }
}
