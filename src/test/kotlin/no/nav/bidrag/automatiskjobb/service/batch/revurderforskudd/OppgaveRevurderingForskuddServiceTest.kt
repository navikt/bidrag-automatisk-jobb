package no.nav.bidrag.automatiskjobb.service.batch.revurderforskudd

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import no.nav.bidrag.automatiskjobb.service.OppgaveService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class OppgaveRevurderingForskuddServiceTest {
    @MockK(relaxed = true)
    private lateinit var oppgaveService: OppgaveService

    @MockK(relaxed = true)
    private lateinit var revurderForskuddRepository: RevurderForskuddRepository

    @InjectMockKs
    private lateinit var oppgaveRevurderingForskuddService: OppgaveRevurderingForskuddService

    @Test
    fun `skal lagre oppgave ID til revurderingForskudd og returnere den`() {
        every { oppgaveService.opprettOppgaveForTilbakekrevingAvForskudd(any()) } returns 12345
        every { revurderForskuddRepository.save(any()) } returnsArgument 0

        val revurderingForskudd = mockk<RevurderingForskudd>(relaxed = true)
        val resultat = oppgaveRevurderingForskuddService.opprettOppgave(revurderingForskudd)

        resultat shouldBe 12345
        verify(exactly = 1) { oppgaveService.opprettOppgaveForTilbakekrevingAvForskudd(revurderingForskudd) }
        verify(exactly = 1) { revurderForskuddRepository.save(revurderingForskudd) }
    }

    @Test
    fun `skal kaste exception hvis oppgaveopprettelse feiler`() {
        every { oppgaveService.opprettOppgaveForTilbakekrevingAvForskudd(any()) } throws RuntimeException("Feil ved opprettelse av oppgave")

        val revurderingForskudd = mockk<RevurderingForskudd>(relaxed = true)

        shouldThrow<RuntimeException> {
            oppgaveRevurderingForskuddService.opprettOppgave(revurderingForskudd)
        }.message shouldBe "Feil ved opprettelse av oppgave"

        verify(exactly = 1) { oppgaveService.opprettOppgaveForTilbakekrevingAvForskudd(revurderingForskudd) }
        verify(exactly = 0) { revurderForskuddRepository.save(any()) }
    }
}
