package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.IndeksreguleringBidragService
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.springframework.batch.core.annotation.BeforeStep
import org.springframework.batch.core.step.StepExecution
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
class OpprettIndeksreguleringBidragBatchProcessor(
    private val indeksreguleringBidragService: IndeksreguleringBidragService,
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
            val eksisterende = indeksreguleringRepository.findBySaksnummerAndStønadstypeAndÅr(saksnummer, Stønadstype.BIDRAG, år)
            if (eksisterende != null) {
                if (eksisterende.gjennomfort) {
                    LOGGER.info {
                        "Sak $saksnummer er allerede gjennomført for indeksregulering av bidrag for år $år. Hopper over."
                    }
                } else {
                    LOGGER.info {
                        "Sak $saksnummer har en ventende indeksregulering av bidrag for år $år. Hopper over – gjennomføringsbatchen tar seg av den."
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
                    stønadstype = Stønadstype.BIDRAG,
                    status = Status.UBEHANDLET,
                )

            indeksreguleringBidragService.indeksregulerBidrag(indeksregulering, barn)
        } catch (e: Exception) {
            LOGGER.error(e) {
                "Det skjedde en feil ved oppretting av indeksregulering bidrag for sak $saksnummer. Hopper over saken."
            }
            null
        }
    }
}
