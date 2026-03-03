package no.nav.bidrag.automatiskjobb.service.oppgave

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.OppgaveConsumer
import no.nav.bidrag.automatiskjobb.consumer.dto.OppgaveDto
import no.nav.bidrag.automatiskjobb.consumer.dto.OppgaveSokResponse
import no.nav.bidrag.automatiskjobb.consumer.dto.OppgaveType
import no.nav.bidrag.automatiskjobb.consumer.dto.behandlingstypeNasjonal
import no.nav.bidrag.automatiskjobb.consumer.dto.formatterDatoForOppgave
import no.nav.bidrag.automatiskjobb.domene.BarnEndretOpplysning
import no.nav.bidrag.automatiskjobb.domene.BarnetrygdBisysMelding
import no.nav.bidrag.automatiskjobb.domene.BarnetrygdEndretType
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.automatiskjobb.service.BaksOpphørBarnetrygdService
import no.nav.bidrag.automatiskjobb.service.OppgaveService
import no.nav.bidrag.automatiskjobb.service.RevurderForskuddService
import no.nav.bidrag.automatiskjobb.service.model.ForskuddRedusertResultat
import no.nav.bidrag.automatiskjobb.testdata.SAKSBEHANDLER_IDENT
import no.nav.bidrag.automatiskjobb.testdata.opprettVedtakhendelse
import no.nav.bidrag.automatiskjobb.testdata.personIdentBidragsmottaker
import no.nav.bidrag.automatiskjobb.testdata.personIdentBidragspliktig
import no.nav.bidrag.automatiskjobb.testdata.personIdentSøknadsbarn1
import no.nav.bidrag.automatiskjobb.testdata.personIdentSøknadsbarn2
import no.nav.bidrag.automatiskjobb.testdata.saksnummer
import no.nav.bidrag.automatiskjobb.utils.revurderForskuddBeskrivelse
import no.nav.bidrag.commons.util.VirkedagerProvider
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.behandling.vedtak.Stønadsendring
import no.nav.bidrag.transport.sak.BidragssakDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
class OppgaveRevurderForskuddOgBidragTest {
    lateinit var oppgaveService: OppgaveService

    @MockK
    lateinit var bidragSakConsumer: BidragSakConsumer

    @MockK
    lateinit var oppgaveConsumer: OppgaveConsumer

    @MockK
    lateinit var revurderForskuddService: RevurderForskuddService

    @MockK
    lateinit var barnRepository: BarnRepository

//    @MockK
    lateinit var baksOpphørBarnetrygdService: BaksOpphørBarnetrygdService

    val beskrivelse =
        "Barnetrygd er opphørt/redusert manuelt fra og med ${YearMonth.now()} i denne saken for barnet med fødselsnummer " +
            "$personIdentSøknadsbarn1. Vurder om bidrag eller forskudd også skal stoppes."

    @BeforeEach
    fun setUp() {
        every { bidragSakConsumer.hentSak(any()) } returns
            BidragssakDto(
                eierfogd = Enhetsnummer("4806"),
                saksnummer = Saksnummer("123213"),
                saksstatus = Bidragssakstatus.IN,
                kategori = Sakskategori.N,
                opprettetDato = LocalDate.now(),
                levdeAdskilt = false,
                ukjentPart = false,
            )

        oppgaveService = OppgaveService(oppgaveConsumer, bidragSakConsumer, revurderForskuddService)
        baksOpphørBarnetrygdService = BaksOpphørBarnetrygdService(barnRepository, oppgaveService)
    }

    @Disabled
    @Test
    fun `skal opprette oppgave for å revurdere forskudd og bidrag etter opphør av barnetrygd`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()

        oppgaveService.opprettRevurderForskuddOgBidragOppgave(
            saksnummer = saksnummer,
            mottaker = personIdentBidragsmottaker,
            kravhaver = personIdentSøknadsbarn1,
            fom = YearMonth.now(),
        )
        verify(exactly = 1) {
            oppgaveConsumer.hentOppgave(
                withArg {
                    it.hentParametre() shouldContain "oppgavetype=GEN"
                    it.hentParametre() shouldContain "saksreferanse=$saksnummer"
                    it.hentParametre() shouldContain "tema=BID"
                },
            )
        }
        verify(exactly = 1) {
            oppgaveConsumer.opprettOppgave(
                withArg {
                    it.saksreferanse shouldBe saksnummer
                    it.tema shouldBe "BID"
                    it.personident shouldBe personIdentBidragsmottaker
                    it.oppgavetype shouldBe OppgaveType.GEN
                    it.tildeltEnhetsnr shouldBe "4806"
                    it.beskrivelse.shouldContain(beskrivelse)
                },
            )
        }
    }

    @Test
    fun `skal ikke opprette oppgave for å revurdere forskudd og bidrag hvis oppgave finnes fra før`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns
            OppgaveSokResponse(
                1,
                listOf(
                    OppgaveDto(
                        1,
                        beskrivelse = beskrivelse,
                    ),
                ),
            )
        oppgaveService.opprettRevurderForskuddOgBidragOppgave(
            saksnummer = saksnummer,
            mottaker = personIdentBidragsmottaker,
            kravhaver = personIdentSøknadsbarn1,
            fom = YearMonth.now(),
        )

        verify(exactly = 0) {
            oppgaveConsumer.opprettOppgave(any())
        }
    }

    @Disabled
    @Test
    fun `skal opprette oppgave for å revurdere forskudd og bidrag hvis det finnes forskudd eller bidrag for barn`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
        every { barnRepository.finnLøpendeForskuddForBarn(any(), any()) } returns emptyList()
        every { barnRepository.finnLøpendeBidragForBarn(any(), any()) } returns
            listOf(
                Barn(
                    id = 1,
                    kravhaver = personIdentSøknadsbarn1,
                    saksnummer = saksnummer,
                    forskuddFra = LocalDate.now().minusMonths(1),
                    forskuddTil = null,
                ),
            )

        val melding =
            BarnetrygdBisysMelding(
                søker = personIdentBidragsmottaker,
                barn =
                    listOf(
                        BarnEndretOpplysning(
                            ident = personIdentSøknadsbarn1,
                            årsakskode = BarnetrygdEndretType.RO,
                            fom = YearMonth.now(),
                        ),
                    ),
            )

        baksOpphørBarnetrygdService.behandleBarnetrygdHendelse(melding)

        verify(exactly = 1) {
            oppgaveConsumer.opprettOppgave(
                withArg {
                    it.saksreferanse shouldBe saksnummer
                    it.tema shouldBe "BID"
                    it.personident shouldBe personIdentBidragsmottaker
                    it.oppgavetype shouldBe OppgaveType.GEN
                    it.tildeltEnhetsnr shouldBe "4806"
                    it.beskrivelse.shouldContain(beskrivelse)
                },
            )
        }
    }

    @Test
    fun `skal ikke opprette oppgave for å revurdere forskudd og bidrag hvis det ikke finnes forskudd eller bidrag for barn`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
        every { barnRepository.finnLøpendeForskuddForBarn(any(), any()) } returns emptyList()
        every { barnRepository.finnLøpendeBidragForBarn(any(), any()) } returns emptyList()

        val melding =
            BarnetrygdBisysMelding(
                søker = personIdentBidragsmottaker,
                barn =
                    listOf(
                        BarnEndretOpplysning(
                            ident = personIdentSøknadsbarn1,
                            årsakskode = BarnetrygdEndretType.RO,
                            fom = YearMonth.now(),
                        ),
                    ),
            )

        baksOpphørBarnetrygdService.behandleBarnetrygdHendelse(melding)

        verify(exactly = 0) {
            oppgaveConsumer.opprettOppgave(any())
        }
    }
}
