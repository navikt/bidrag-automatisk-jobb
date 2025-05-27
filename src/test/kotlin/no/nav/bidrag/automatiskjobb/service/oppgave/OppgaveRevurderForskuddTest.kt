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
import no.nav.bidrag.automatiskjobb.consumer.dto.behandlingstypeUtland
import no.nav.bidrag.automatiskjobb.consumer.dto.formatterDatoForOppgave
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
import no.nav.bidrag.automatiskjobb.testdata.stubSaksbehandlernavnProvider
import no.nav.bidrag.automatiskjobb.utils.revurderForskuddBeskrivelse
import no.nav.bidrag.automatiskjobb.utils.revurderForskuddBeskrivelseSærbidrag
import no.nav.bidrag.commons.util.VirkedagerProvider
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.felles.personidentNav
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.behandling.vedtak.Engangsbeløp
import no.nav.bidrag.transport.behandling.vedtak.Stønadsendring
import no.nav.bidrag.transport.sak.BidragssakDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OppgaveRevurderForskuddTest {
    lateinit var oppgaveService: OppgaveService

    @MockK
    lateinit var bidragSakConsumer: BidragSakConsumer

    @MockK
    lateinit var oppgaveConsumer: OppgaveConsumer

    @MockK
    lateinit var revurderForskuddService: RevurderForskuddService

    @BeforeEach
    fun setUp() {
        stubSaksbehandlernavnProvider()
        every { revurderForskuddService.erForskuddRedusert(any()) } returns
            listOf(
                ForskuddRedusertResultat(
                    saksnummer = saksnummer,
                    bidragsmottaker = personIdentBidragsmottaker,
                    gjelderBarn = personIdentSøknadsbarn1,
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
        oppgaveService = OppgaveService(oppgaveConsumer, bidragSakConsumer, revurderForskuddService)
    }

    @Test
    fun `skal opprette revurder forskudd oppgave`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
        oppgaveService.opprettRevurderForskuddOppgave(
            opprettVedtakhendelse(1),
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
                    it.aktivDato shouldBe formatterDatoForOppgave(LocalDate.now())
                    it.fristFerdigstillelse shouldBe formatterDatoForOppgave(VirkedagerProvider.nesteVirkedag())
                    it.personident shouldBe personIdentBidragsmottaker
                    it.oppgavetype shouldBe OppgaveType.GEN
                    it.tildeltEnhetsnr shouldBe "4806"
                    it.tilordnetRessurs shouldBe SAKSBEHANDLER_IDENT
                    it.behandlingstype.shouldBe(behandlingstypeNasjonal)
                    it.beskrivelse.shouldContain(revurderForskuddBeskrivelse)
                },
            )
        }
    }

    @Test
    fun `skal opprette revurder forskudd oppgave særbidrag`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
        every { revurderForskuddService.erForskuddRedusert(any()) } returns
            listOf(
                ForskuddRedusertResultat(
                    saksnummer = saksnummer,
                    bidragsmottaker = personIdentBidragsmottaker,
                    gjelderBarn = personIdentSøknadsbarn1,
                    engangsbeløptype = Engangsbeløptype.SÆRBIDRAG,
                ),
            )
        oppgaveService.opprettRevurderForskuddOppgave(
            opprettVedtakhendelse(1).copy(
                stønadsendringListe = emptyList(),
                engangsbeløpListe =
                    listOf(
                        Engangsbeløp(
                            type = Engangsbeløptype.SÆRBIDRAG,
                            kravhaver = Personident(personIdentSøknadsbarn1),
                            mottaker = Personident(personIdentBidragsmottaker),
                            skyldner = Personident(personIdentBidragspliktig),
                            sak = Saksnummer(saksnummer),
                            innkreving = Innkrevingstype.MED_INNKREVING,
                            beslutning = Beslutningstype.ENDRING,
                            omgjørVedtakId = null,
                            eksternReferanse = null,
                            resultatkode = Resultatkode.SÆRBIDRAG_INNVILGET.name,
                            beløp = BigDecimal(1000),
                            valutakode = "",
                            referanse = "",
                            delytelseId = "",
                        ),
                    ),
            ),
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
                    it.aktivDato shouldBe formatterDatoForOppgave(LocalDate.now())
                    it.fristFerdigstillelse shouldBe formatterDatoForOppgave(VirkedagerProvider.nesteVirkedag())
                    it.personident shouldBe personIdentBidragsmottaker
                    it.oppgavetype shouldBe OppgaveType.GEN
                    it.tildeltEnhetsnr shouldBe "4806"
                    it.tilordnetRessurs shouldBe SAKSBEHANDLER_IDENT
                    it.behandlingstype.shouldBe(behandlingstypeNasjonal)
                    it.beskrivelse.shouldContain(revurderForskuddBeskrivelseSærbidrag)
                },
            )
        }
    }

    @Test
    fun `skal opprette revurder forskudd oppgave på sak eierfogd`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
        every { bidragSakConsumer.hentSak(any()) } returns
            BidragssakDto(
                eierfogd = Enhetsnummer("4833"),
                saksnummer = Saksnummer("123213"),
                saksstatus = Bidragssakstatus.IN,
                kategori = Sakskategori.N,
                opprettetDato = LocalDate.now(),
                levdeAdskilt = false,
                ukjentPart = false,
            )
        oppgaveService.opprettRevurderForskuddOppgave(
            opprettVedtakhendelse(1),
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
                    it.aktivDato shouldBe formatterDatoForOppgave(LocalDate.now())
                    it.fristFerdigstillelse shouldBe formatterDatoForOppgave(VirkedagerProvider.nesteVirkedag())
                    it.personident shouldBe personIdentBidragsmottaker
                    it.oppgavetype shouldBe OppgaveType.GEN
                    it.tildeltEnhetsnr shouldBe "4833"
                    it.tilordnetRessurs shouldBe null
                    it.behandlingstype.shouldBe(behandlingstypeNasjonal)
                    it.beskrivelse.shouldContain(revurderForskuddBeskrivelse)
                },
            )
        }
    }

    @Test
    fun `skal opprette revurder forskudd oppgave hvis det er klageenhet`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
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
        oppgaveService.opprettRevurderForskuddOppgave(
            opprettVedtakhendelse(1).copy(
                enhetsnummer = Enhetsnummer("4291"),
            ),
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
                    it.aktivDato shouldBe formatterDatoForOppgave(LocalDate.now())
                    it.fristFerdigstillelse shouldBe formatterDatoForOppgave(VirkedagerProvider.nesteVirkedag())
                    it.personident shouldBe personIdentBidragsmottaker
                    it.oppgavetype shouldBe OppgaveType.GEN
                    it.tildeltEnhetsnr shouldBe "4806"
                    it.tilordnetRessurs shouldBe null
                    it.behandlingstype.shouldBe(behandlingstypeNasjonal)
                    it.beskrivelse.shouldContain(revurderForskuddBeskrivelse)
                },
            )
        }
    }

    @Test
    fun `skal opprette revurder forskudd oppgave for utland`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
        every { bidragSakConsumer.hentSak(any()) } returns
            BidragssakDto(
                eierfogd = Enhetsnummer("4865"),
                saksnummer = Saksnummer("123213"),
                saksstatus = Bidragssakstatus.IN,
                kategori = Sakskategori.N,
                opprettetDato = LocalDate.now(),
                levdeAdskilt = false,
                ukjentPart = false,
            )
        oppgaveService.opprettRevurderForskuddOppgave(
            opprettVedtakhendelse(1).copy(
                enhetsnummer = Enhetsnummer("4865"),
                stønadsendringListe =
                    listOf(
                        Stønadsendring(
                            type = Stønadstype.BIDRAG,
                            eksternReferanse = "",
                            beslutning = Beslutningstype.ENDRING,
                            førsteIndeksreguleringsår = 2024,
                            innkreving = Innkrevingstype.MED_INNKREVING,
                            kravhaver = Personident(personIdentSøknadsbarn1),
                            mottaker = Personident(personIdentBidragsmottaker),
                            omgjørVedtakId = 1,
                            periodeListe = emptyList(),
                            sak = Saksnummer(saksnummer),
                            skyldner = Personident(personIdentBidragspliktig),
                        ),
                    ),
            ),
        )
        verify(exactly = 1) {
            oppgaveConsumer.opprettOppgave(
                withArg {
                    it.saksreferanse shouldBe saksnummer
                    it.tema shouldBe "BID"
                    it.aktivDato shouldBe formatterDatoForOppgave(LocalDate.now())
                    it.fristFerdigstillelse shouldBe formatterDatoForOppgave(VirkedagerProvider.nesteVirkedag())
                    it.personident shouldBe personIdentBidragsmottaker
                    it.oppgavetype shouldBe OppgaveType.GEN
                    it.tildeltEnhetsnr shouldBe "4865"
                    it.tilordnetRessurs shouldBe SAKSBEHANDLER_IDENT
                    it.behandlingstype.shouldBe(behandlingstypeUtland)
                    it.beskrivelse.shouldContain(revurderForskuddBeskrivelse)
                },
            )
        }
    }

    @Test
    fun `skal opprette revurder forskudd oppgave for farskap`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
        every { bidragSakConsumer.hentSak(any()) } returns
            BidragssakDto(
                eierfogd = Enhetsnummer("4860"),
                saksnummer = Saksnummer("123213"),
                saksstatus = Bidragssakstatus.IN,
                kategori = Sakskategori.N,
                opprettetDato = LocalDate.now(),
                levdeAdskilt = false,
                ukjentPart = false,
            )
        oppgaveService.opprettRevurderForskuddOppgave(
            opprettVedtakhendelse(1).copy(
                enhetsnummer = Enhetsnummer("4860"),
            ),
        )
        verify(exactly = 1) {
            oppgaveConsumer.opprettOppgave(
                withArg {
                    it.saksreferanse shouldBe saksnummer
                    it.tema shouldBe "FAR"
                    it.aktivDato shouldBe formatterDatoForOppgave(LocalDate.now())
                    it.fristFerdigstillelse shouldBe formatterDatoForOppgave(VirkedagerProvider.nesteVirkedag())
                    it.personident shouldBe personIdentBidragsmottaker
                    it.oppgavetype shouldBe OppgaveType.GEN
                    it.tildeltEnhetsnr shouldBe "4860"
                    it.tilordnetRessurs shouldBe SAKSBEHANDLER_IDENT
                    it.behandlingstype.shouldBe(behandlingstypeNasjonal)
                    it.beskrivelse.shouldContain(revurderForskuddBeskrivelse)
                },
            )
        }
    }

    @Test
    fun `skal ikke opprette revurder forskudd oppgave hvis ikke bidrag`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
        oppgaveService.opprettRevurderForskuddOppgave(
            opprettVedtakhendelse(1).copy(
                enhetsnummer = Enhetsnummer("4806"),
                stønadsendringListe =
                    listOf(
                        Stønadsendring(
                            type = Stønadstype.FORSKUDD,
                            eksternReferanse = "",
                            beslutning = Beslutningstype.ENDRING,
                            førsteIndeksreguleringsår = 2024,
                            innkreving = Innkrevingstype.MED_INNKREVING,
                            kravhaver = Personident(personIdentSøknadsbarn1),
                            mottaker = Personident(personIdentBidragsmottaker),
                            omgjørVedtakId = 1,
                            periodeListe = emptyList(),
                            sak = Saksnummer(saksnummer),
                            skyldner = personidentNav,
                        ),
                    ),
            ),
        )

        verify(exactly = 0) {
            oppgaveConsumer.opprettOppgave(any())
        }
        verify(exactly = 0) {
            oppgaveConsumer.hentOppgave(any())
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
                            "--- 20.02.2025 06:59 F_Z994977 E_Z994977 (Z994977, 4806) ---\r\ndsad" +
                                "\r\n\r\n--- 20.02.2025 06:59 Z994977 ---\r\n$revurderForskuddBeskrivelse\r\n\r\n",
                    ),
                ),
            )
        oppgaveService.opprettRevurderForskuddOppgave(
            opprettVedtakhendelse(1).copy(
                enhetsnummer = Enhetsnummer("4806"),
            ),
        )

        verify(exactly = 0) {
            oppgaveConsumer.opprettOppgave(any())
        }
    }

    @Test
    fun `skal ikke opprette revurder forskudd oppgave hvis ingen løpende forskudd`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
        every { revurderForskuddService.erForskuddRedusert(any()) } returns listOf()
        oppgaveService.opprettRevurderForskuddOppgave(
            opprettVedtakhendelse(1).copy(
                enhetsnummer = Enhetsnummer("4806"),
                stønadsendringListe =
                    listOf(
                        Stønadsendring(
                            type = Stønadstype.BIDRAG,
                            eksternReferanse = "",
                            beslutning = Beslutningstype.ENDRING,
                            førsteIndeksreguleringsår = 2024,
                            innkreving = Innkrevingstype.MED_INNKREVING,
                            kravhaver = Personident(personIdentSøknadsbarn1),
                            mottaker = Personident(personIdentBidragsmottaker),
                            omgjørVedtakId = 1,
                            periodeListe = emptyList(),
                            sak = Saksnummer(saksnummer),
                            skyldner = Personident(personIdentBidragspliktig),
                        ),
                    ),
            ),
        )

        verify(exactly = 0) {
            oppgaveConsumer.opprettOppgave(any())
        }
    }

    @Test
    fun `skal opprette bare en revurder forskudd oppgave hvis flere barn har løpende forskudd som er redusert`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
        every { revurderForskuddService.erForskuddRedusert(any()) } returns
            listOf(
                ForskuddRedusertResultat(
                    saksnummer = saksnummer,
                    bidragsmottaker = personIdentBidragsmottaker,
                    gjelderBarn = personIdentSøknadsbarn1,
                ),
                ForskuddRedusertResultat(
                    saksnummer = saksnummer,
                    bidragsmottaker = personIdentBidragsmottaker,
                    gjelderBarn = personIdentSøknadsbarn2,
                ),
            )
        oppgaveService.opprettRevurderForskuddOppgave(
            opprettVedtakhendelse(1),
        )

        verify(exactly = 1) {
            oppgaveConsumer.opprettOppgave(
                withArg {
                    it.saksreferanse shouldBe saksnummer
                    it.tema shouldBe "BID"
                    it.aktivDato shouldBe formatterDatoForOppgave(LocalDate.now())
                    it.fristFerdigstillelse shouldBe formatterDatoForOppgave(VirkedagerProvider.nesteVirkedag())
                    it.personident shouldBe personIdentBidragsmottaker
                    it.oppgavetype shouldBe OppgaveType.GEN
                    it.tildeltEnhetsnr shouldBe "4806"
                    it.tilordnetRessurs shouldBe SAKSBEHANDLER_IDENT
                    it.behandlingstype.shouldBe(behandlingstypeNasjonal)
                    it.beskrivelse.shouldContain(revurderForskuddBeskrivelse)
                },
            )
        }
    }

    @Test
    fun `skal ikke opprette revurder forskudd oppgave hvis vedtak er fattet av batchkjøring`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
        oppgaveService.opprettRevurderForskuddOppgave(
            opprettVedtakhendelse(1).copy(
                kilde = Vedtakskilde.AUTOMATISK,
                enhetsnummer = Enhetsnummer("4806"),
                stønadsendringListe =
                    listOf(
                        Stønadsendring(
                            type = Stønadstype.BIDRAG,
                            eksternReferanse = "",
                            beslutning = Beslutningstype.ENDRING,
                            førsteIndeksreguleringsår = 2024,
                            innkreving = Innkrevingstype.MED_INNKREVING,
                            kravhaver = Personident(personIdentSøknadsbarn1),
                            mottaker = Personident(personIdentBidragsmottaker),
                            omgjørVedtakId = 1,
                            periodeListe = emptyList(),
                            sak = Saksnummer(saksnummer),
                            skyldner = Personident(personIdentBidragspliktig),
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
    fun `skal ikke opprette revurder forskudd oppgave hvis forskudd`() {
        every { oppgaveConsumer.opprettOppgave(any()) } returns OppgaveDto(1)
        every { oppgaveConsumer.hentOppgave(any()) } returns OppgaveSokResponse()
        oppgaveService.opprettRevurderForskuddOppgave(
            opprettVedtakhendelse(1).copy(
                enhetsnummer = Enhetsnummer("4806"),
                stønadsendringListe =
                    listOf(
                        Stønadsendring(
                            type = Stønadstype.FORSKUDD,
                            eksternReferanse = "",
                            beslutning = Beslutningstype.ENDRING,
                            førsteIndeksreguleringsår = 2024,
                            innkreving = Innkrevingstype.MED_INNKREVING,
                            kravhaver = Personident(personIdentSøknadsbarn1),
                            mottaker = Personident(personIdentBidragsmottaker),
                            omgjørVedtakId = 1,
                            periodeListe = emptyList(),
                            sak = Saksnummer(saksnummer),
                            skyldner = Personident(personIdentBidragspliktig),
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
}
