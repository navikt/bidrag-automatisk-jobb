package no.nav.bidrag.automatiskjobb.service.oppgave

import io.getunleash.FakeUnleash
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
import no.nav.bidrag.automatiskjobb.domene.Endringsmelding
import no.nav.bidrag.automatiskjobb.service.OppgaveService
import no.nav.bidrag.automatiskjobb.service.RevurderForskuddService
import no.nav.bidrag.automatiskjobb.service.model.AdresseEndretResultat
import no.nav.bidrag.automatiskjobb.service.opprettRevurderForskuddOppgaveToggleName
import no.nav.bidrag.automatiskjobb.testdata.saksnummer
import no.nav.bidrag.automatiskjobb.testdata.stubSaksbehandlernavnProvider
import no.nav.bidrag.automatiskjobb.testdata.testdataBidragsmottaker
import no.nav.bidrag.automatiskjobb.testdata.testdataEnhet
import no.nav.bidrag.automatiskjobb.testdata.testdataSøknadsbarn1
import no.nav.bidrag.automatiskjobb.utils.revurderForskuddBeskrivelseAdresseendring
import no.nav.bidrag.commons.util.VirkedagerProvider
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.sak.BidragssakDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OppgaveRevurderForskuddAdresseendringTest {
    lateinit var oppgaveService: OppgaveService

    @MockK
    lateinit var bidragSakConsumer: BidragSakConsumer

    @MockK
    lateinit var oppgaveConsumer: OppgaveConsumer

    @MockK
    lateinit var revurderForskuddService: RevurderForskuddService

    val unleash = FakeUnleash()

    @BeforeEach
    fun setUp() {
        unleash.enable(opprettRevurderForskuddOppgaveToggleName)
        stubSaksbehandlernavnProvider()
        every { revurderForskuddService.skalBMFortsattMottaForskuddForSøknadsbarnEtterAdresseendring(any()) } returns
            listOf(
                AdresseEndretResultat(
                    saksnummer = saksnummer,
                    bidragsmottaker = testdataBidragsmottaker.personIdent.verdi,
                    gjelderBarn = testdataSøknadsbarn1.personIdent.verdi,
                    enhet = testdataEnhet,
                ),
            )
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
        oppgaveService = OppgaveService(oppgaveConsumer, bidragSakConsumer, revurderForskuddService, unleash)
    }

    @Test
    fun `skal opprette revurder forskudd oppgave`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
        oppgaveService.sjekkOgOpprettRevurderForskuddOppgaveEtterBarnFlyttetFraBM(
            Endringsmelding(
                aktørid = testdataSøknadsbarn1.personIdent.verdi,
                personidenter = emptySet(),
                endringer =
                    listOf(
                        Endringsmelding.Endring(
                            adresseendring =
                                Endringsmelding.Adresseendring(
                                    type = Endringsmelding.Opplysningstype.OPPHOLDSADRESSE,
                                ),
                        ),
                    ),
            ),
        )
        verify(exactly = 1) {
            oppgaveConsumer.hentOppgave(
                withArg {
                    it.hentParametre() shouldContain "oppgavetype=GEN"
                    it.hentParametre() shouldContain "saksreferanse=$saksnummer"
                    it.hentParametre() shouldContain "aktoerId=${testdataSøknadsbarn1.personIdent.verdi}"
                    it.hentParametre() shouldContain "tema=BID"
                },
            )
        }
        verify(exactly = 1) {
            oppgaveConsumer.opprettOppgave(
                withArg {
                    it.saksreferanse shouldBe saksnummer
                    it.tema shouldBe "BID"
                    it.aktivDato shouldBe formatterDatoForOppgave(LocalDate.now())
                    it.fristFerdigstillelse shouldBe formatterDatoForOppgave(VirkedagerProvider.nesteVirkedag())
                    it.personident shouldBe testdataSøknadsbarn1.personIdent.verdi
                    it.oppgavetype shouldBe OppgaveType.GEN
                    it.tildeltEnhetsnr shouldBe testdataEnhet
                    it.tilordnetRessurs shouldBe null
                    it.saksreferanse shouldBe saksnummer
                    it.behandlingstype.shouldBe(behandlingstypeNasjonal)
                    it.beskrivelse.shouldContain(revurderForskuddBeskrivelseAdresseendring)
                },
            )
        }
    }

    @Test
    fun `skal ikke opprette revurder forskudd oppgave hvis feature toggle er av`() {
        unleash.disable(opprettRevurderForskuddOppgaveToggleName)
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
        oppgaveService.sjekkOgOpprettRevurderForskuddOppgaveEtterBarnFlyttetFraBM(
            Endringsmelding(
                aktørid = testdataSøknadsbarn1.personIdent.verdi,
                personidenter = emptySet(),
                endringer =
                    listOf(
                        Endringsmelding.Endring(
                            adresseendring =
                                Endringsmelding.Adresseendring(
                                    type = Endringsmelding.Opplysningstype.OPPHOLDSADRESSE,
                                ),
                        ),
                    ),
            ),
        )
        verify(exactly = 0) {
            oppgaveConsumer.hentOppgave(any())
        }
        verify(exactly = 0) {
            oppgaveConsumer.opprettOppgave(any())
        }
    }

    @Test
    fun `skal ikke opprette revurder forskudd oppgave hvis finnes fra før`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns
            OppgaveSokResponse(
                1,
                listOf(
                    OppgaveDto(
                        1,
                        beskrivelse =
                            "--- 20.02.2025 06:59 Automatisk jobb ---\r\ndsad" +
                                "\r\n\r\n--- 20.02.2025 06:59 Z994977 ---\r\n$revurderForskuddBeskrivelseAdresseendring\r\n\r\n",
                    ),
                ),
            )
        oppgaveService.sjekkOgOpprettRevurderForskuddOppgaveEtterBarnFlyttetFraBM(
            Endringsmelding(
                aktørid = testdataSøknadsbarn1.personIdent.verdi,
                personidenter = emptySet(),
                endringer =
                    listOf(
                        Endringsmelding.Endring(
                            adresseendring =
                                Endringsmelding.Adresseendring(
                                    type = Endringsmelding.Opplysningstype.OPPHOLDSADRESSE,
                                ),
                        ),
                    ),
            ),
        )

        verify(exactly = 0) {
            oppgaveConsumer.opprettOppgave(any())
        }
    }

    @Test
    fun `skal ikke opprette sjekke eller opprette revurder forskudd hvis hendelse ikke er adresseendring`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
        oppgaveService.sjekkOgOpprettRevurderForskuddOppgaveEtterBarnFlyttetFraBM(
            Endringsmelding(
                aktørid = testdataSøknadsbarn1.personIdent.verdi,
                personidenter = emptySet(),
                endringer =
                    listOf(
                        Endringsmelding.Endring(
                            opplysningstype = Endringsmelding.Opplysningstype.FOLKEREGISTERIDENTIFIKATOR,
                            identendring = Endringsmelding.Identendring(),
                        ),
                    ),
            ),
        )

        verify(exactly = 0) {
            revurderForskuddService.skalBMFortsattMottaForskuddForSøknadsbarnEtterAdresseendring(any())
        }
        verify(exactly = 0) {
            oppgaveConsumer.opprettOppgave(any())
        }
    }
}
