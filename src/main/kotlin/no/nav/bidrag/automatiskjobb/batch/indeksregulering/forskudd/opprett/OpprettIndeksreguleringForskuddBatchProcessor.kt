package no.nav.bidrag.automatiskjobb.batch.indeksregulering.forskudd.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.IndeksreguleringForskuddService
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.core.step.StepExecution
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
class OpprettIndeksreguleringForskuddBatchProcessor(
    private val indeksreguleringForskuddService: IndeksreguleringForskuddService,
    private val indeksreguleringRepository: IndeksreguleringRepository,
) : ItemProcessor<List<Barn>, Indeksregulering> {
    private lateinit var batchId: String
    private var år: Int = 0

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        batchId = stepExecution.jobParameters.getString("batchId")!!
        år = stepExecution.jobParameters.getString("aar")!!.toInt()
    }

    override fun process(barn: List<Barn>): Indeksregulering? {
        val saksnummer = barn.first().saksnummer
        return try {
            val eksisterende = indeksreguleringRepository.findBySaksnummerAndStønadstypeAndÅr(saksnummer, Stønadstype.FORSKUDD, år)
            if (eksisterende != null) {
                if (eksisterende.gjennomfort) {
                    LOGGER.info {
                        "Sak $saksnummer er allerede gjennomført for indeksregulering av forskudd for år $år. Hopper over."
                    }
                } else {
                    LOGGER.info {
                        "Sak $saksnummer har en ventende indeksregulering av forskudd for år $år. Hopper over – gjennomføringsbatchen tar seg av den."
                    }
                }
                return null
            }

            val indeksregulering =
                Indeksregulering(
                    batchId = batchId,
                    saksnummer = saksnummer,
                    år = år,
                    barn = barn.toMutableList(),
                    stønadstype = Stønadstype.FORSKUDD,
                    status = Status.UBEHANDLET,
                )

            indeksreguleringForskuddService.indeksregulerForskudd(indeksregulering, barn)
        } catch (e: Exception) {
            LOGGER.error(e) {
                "Det skjedde en feil ved oppretting av indeksregulering forskudd for sak $saksnummer. Hopper over saken."
            }
            null
        }
    }
}
