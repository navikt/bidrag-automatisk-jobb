package no.nav.bidrag.aldersjustering.service

import no.nav.bidrag.aldersjustering.persistence.entity.Barn
import no.nav.bidrag.aldersjustering.persistence.repository.BarnRepository
import org.springframework.stereotype.Service

@Service
class AldersjusteringService(
    private val barnRepository: BarnRepository,
) {
    fun hentAlleBarnSomSkalAldersjusteresForÅr(år: Int): Map<Int, List<Barn>> =
        barnRepository
            .finnBarnSomSkalAldersjusteresForÅr(år)
            .groupBy { år - it.fødselsdato.year }
            .mapValues { it.value.sortedBy { barn -> barn.fødselsdato } }
}
