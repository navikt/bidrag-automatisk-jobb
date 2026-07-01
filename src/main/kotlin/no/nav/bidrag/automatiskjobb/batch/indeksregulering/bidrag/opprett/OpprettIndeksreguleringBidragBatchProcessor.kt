package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import no.nav.bidrag.automatiskjobb.service.batch.indeksregulering.IndeksreguleringBidragService
import no.nav.bidrag.commons.util.secureLogger
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
) : ItemProcessor<Barn, List<Indeksregulering>> {
    private lateinit var batchId: String
    private var år: Int = 0

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        batchId = stepExecution.jobParameters.getString("batchId")!!
        år = stepExecution.jobParameters.getString("aar")!!.toInt()
    }

    override fun process(barn: Barn): List<Indeksregulering>? {
        val saksnummer = barn.saksnummer
        val alleStønadstyper = listOf(Stønadstype.BIDRAG, Stønadstype.BIDRAG18AAR, Stønadstype.OPPFOSTRINGSBIDRAG)
        return try {
            val stønadstyper =
                alleStønadstyper.filter { stønadstype ->
                    indeksreguleringRepository.findByBarnAndStønadstypeAndÅr(barn, stønadstype, år) == null
                }

            if (stønadstyper.isEmpty()) {
                secureLogger.info {
                    "Sak $saksnummer for barn: $barn har allerede indeksregulering for alle stønadstyper for år $år. Hopper over."
                }
                return null
            }

            indeksreguleringBidragService.opprettIndeksregulering(batchId, år, barn, stønadstyper)
        } catch (e: Exception) {
            LOGGER.error(e) {
                "Det skjedde en feil ved oppretting av indeksregulering bidrag for sak $saksnummer. Hopper over saken."
            }
            null
        }
    }
}
