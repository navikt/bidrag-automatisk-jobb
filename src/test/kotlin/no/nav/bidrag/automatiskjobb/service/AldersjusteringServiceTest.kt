package no.nav.bidrag.automatiskjobb.service

import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.mapper.VedtakMapper
import no.nav.bidrag.automatiskjobb.persistence.entity.Aldersjustering
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.AldersjusteringRepository
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.beregn.barnebidrag.service.AldersjusteringOrchestrator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class AldersjusteringServiceTest {
    @RelaxedMockK
    lateinit var barnRepository: BarnRepository

    @RelaxedMockK
    lateinit var aldersjusteringRepository: AldersjusteringRepository

    @RelaxedMockK
    lateinit var vedtakConsumer: BidragVedtakConsumer

    @RelaxedMockK
    lateinit var sakConsumer: BidragSakConsumer

    @RelaxedMockK
    lateinit var aldersjusteringOrchestrator: AldersjusteringOrchestrator

    @RelaxedMockK
    lateinit var vedtakMapper: VedtakMapper

    @RelaxedMockK
    lateinit var oppgaveService: OppgaveService

    @RelaxedMockK
    lateinit var forsendelseBestillingService: ForsendelseBestillingService

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

    @Test
    fun `skal opprette aldersjustering for barn`() {
        val barn = Barn(id = 1, fødselsdato = LocalDate.now().minusYears(6))
        every { aldersjusteringRepository.existsAldersjusteringsByBarnAndAldersgruppe(Barn(), 6) } returns false
        every { aldersjusteringRepository.save(any()) } returns
            Aldersjustering(id = 1, barn = Barn(), aldersgruppe = 6, status = Status.UBEHANDLET, batchId = "batch-1")

        aldersjusteringService.opprettAldersjusteringForBarn(barn, LocalDate.now().year, "batch-1")

        verify { aldersjusteringRepository.save(any()) }
    }

    @Test
    fun `skal ikke opprette aldersjustering hvis den allerede finnes`() {
        val barn = Barn(id = 1, fødselsdato = LocalDate.now().minusYears(6))
        every { aldersjusteringRepository.existsAldersjusteringsByBarnAndAldersgruppe(Barn(), 6) } returns true

        aldersjusteringService.opprettAldersjusteringForBarn(barn, LocalDate.now().year, "batch-1")

        verify(exactly = 0) { aldersjusteringRepository.save(any()) }
    }

    @Test
    fun `skal hente aldersjustering ved id`() {
        val aldersjustering = Aldersjustering(id = 1, barn = Barn(), aldersgruppe = 6, status = Status.UBEHANDLET, batchId = "batch-1")
        every { aldersjusteringRepository.findById(1) } returns java.util.Optional.of(aldersjustering)

        val result = aldersjusteringService.hentAldersjustering(1)

        result shouldBe aldersjustering
    }

    @Test
    fun `skal returnere null hvis aldersjustering ikke finnes`() {
        every { aldersjusteringRepository.findById(1) } returns java.util.Optional.empty()

        val result = aldersjusteringService.hentAldersjustering(1)

        result shouldBe null
    }

    @Test
    fun `skal lagre aldersjustering`() {
        val aldersjustering = Aldersjustering(id = null, barn = Barn(), aldersgruppe = 6, status = Status.UBEHANDLET, batchId = "batch-1")
        every { aldersjusteringRepository.save(aldersjustering) } returns aldersjustering.copy(id = 1)

        val result = aldersjusteringService.lagreAldersjustering(aldersjustering)

        result shouldBe 1
    }
}
