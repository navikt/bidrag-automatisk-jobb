package no.nav.bidrag.automatiskjobb.service

import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class AldersjusteringServiceTest {
    @RelaxedMockK
    lateinit var barnRepository: BarnRepository

    @InjectMockKs
    lateinit var aldersjusteringService: AldersjusteringService

    @Test
    fun skalHenteAlleBarnSomSkalAldersjusteresFor2024() {
        every { barnRepository.finnBarnSomSkalAldersjusteresForÅr(2024) } returns
            PageImpl(
                listOf(
                    Barn(fødselsdato = LocalDate.of(2018, 1, 1)),
                    Barn(fødselsdato = LocalDate.of(2013, 12, 31)),
                    Barn(fødselsdato = LocalDate.of(2009, 6, 15)),
                ),
            )
        val barnSomSkalAldersjusteres = aldersjusteringService.hentAlleBarnSomSkalAldersjusteresForÅr(2024)

        barnSomSkalAldersjusteres.forEach { (alder, barn) ->
            when (alder) {
                6 -> barn.size shouldBe 1
                11 -> barn.size shouldBe 1
                15 -> barn.size shouldBe 1
            }
        }
    }

    @Test
    fun skalReturnereTomListeOmIngenBarnSkalAldersjusteres() {
        every { barnRepository.finnBarnSomSkalAldersjusteresForÅr(2024) } returns Page.empty()

        val barnSomSkalAldersjusteres = aldersjusteringService.hentAlleBarnSomSkalAldersjusteresForÅr(2024)
        barnSomSkalAldersjusteres shouldHaveSize 0
    }
}
