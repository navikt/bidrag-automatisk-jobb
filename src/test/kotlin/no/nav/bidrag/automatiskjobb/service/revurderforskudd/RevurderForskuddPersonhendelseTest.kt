package no.nav.bidrag.automatiskjobb.service.revurderforskudd

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragSakConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.persistence.repository.RevurderForskuddRepository
import no.nav.bidrag.automatiskjobb.service.ReskontroService
import no.nav.bidrag.automatiskjobb.service.RevurderForskuddService
import no.nav.bidrag.automatiskjobb.testdata.opprettSakRespons
import no.nav.bidrag.automatiskjobb.testdata.opprettStønadDto
import no.nav.bidrag.automatiskjobb.testdata.opprettStønadPeriodeDto
import no.nav.bidrag.automatiskjobb.testdata.personIdentSøknadsbarn1
import no.nav.bidrag.automatiskjobb.testdata.saksnummer
import no.nav.bidrag.automatiskjobb.testdata.testdataBidragsmottaker
import no.nav.bidrag.automatiskjobb.testdata.testdataBidragspliktig
import no.nav.bidrag.automatiskjobb.testdata.testdataEnhet
import no.nav.bidrag.automatiskjobb.testdata.testdataSøknadsbarn1
import no.nav.bidrag.automatiskjobb.testdata.testdataSøknadsbarn2
import no.nav.bidrag.beregn.forskudd.BeregnForskuddApi
import no.nav.bidrag.beregn.vedtak.Vedtaksfiltrering
import no.nav.bidrag.commons.web.mock.stubSjablonProvider
import no.nav.bidrag.commons.web.mock.stubSjablonService
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.person.Husstand
import no.nav.bidrag.transport.person.Husstandsmedlem
import no.nav.bidrag.transport.person.HusstandsmedlemmerDto
import no.nav.bidrag.transport.sak.RolleDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class RevurderForskuddPersonhendelseTest {
    lateinit var service: RevurderForskuddService

    @MockK
    lateinit var bidragBeløpshistorikkConsumer: BidragBeløpshistorikkConsumer

    @MockK
    lateinit var bidragVedtakConsumer: BidragVedtakConsumer

    @MockK
    lateinit var bidragSakConsumer: BidragSakConsumer

    @MockK
    lateinit var bidragPersonConsumer: BidragPersonConsumer

    @MockK
    lateinit var revurderForskuddRepository: RevurderForskuddRepository

    @MockK
    lateinit var reskontroService: ReskontroService

    @BeforeEach
    fun initMocks() {
        // commonObjectmapper.readValue(hentFil("/__files/vedtak_forskudd.json"))

        every { bidragSakConsumer.hentSak(any()) } returns opprettSakRespons()
        stubSjablonService()
        stubSjablonProvider()
        every { bidragSakConsumer.hentSakerForPerson(eq(testdataSøknadsbarn1.personIdent)) } returns
            listOf(
                opprettSakRespons()
                    .copy(
                        roller =
                            listOf(
                                RolleDto(
                                    testdataBidragsmottaker.personIdent,
                                    type = Rolletype.BIDRAGSMOTTAKER,
                                ),
                                RolleDto(
                                    testdataBidragspliktig.personIdent,
                                    type = Rolletype.BIDRAGSPLIKTIG,
                                ),
                                RolleDto(
                                    testdataSøknadsbarn1.personIdent,
                                    type = Rolletype.BARN,
                                ),
                                RolleDto(
                                    testdataSøknadsbarn2.personIdent,
                                    type = Rolletype.BARN,
                                ),
                            ),
                    ),
            )

        service =
            RevurderForskuddService(
                bidragBeløpshistorikkConsumer,
                bidragVedtakConsumer,
                bidragSakConsumer,
                bidragPersonConsumer,
                BeregnForskuddApi(),
                Vedtaksfiltrering(),
                revurderForskuddRepository,
                reskontroService,
            )
    }

    @Test
    fun `skal sjekke og returnere at BM ikke skal motta forskudd etter adresseendring for søknadsbarn som har flere saker`() {
        every { bidragSakConsumer.hentSakerForPerson(eq(testdataSøknadsbarn1.personIdent)) } returns
            listOf(
                opprettSakRespons()
                    .copy(
                        roller =
                            listOf(
                                RolleDto(
                                    testdataBidragsmottaker.personIdent,
                                    type = Rolletype.BIDRAGSMOTTAKER,
                                ),
                                RolleDto(
                                    testdataBidragspliktig.personIdent,
                                    type = Rolletype.BIDRAGSPLIKTIG,
                                ),
                                RolleDto(
                                    testdataSøknadsbarn1.personIdent,
                                    type = Rolletype.BARN,
                                ),
                                RolleDto(
                                    testdataSøknadsbarn2.personIdent,
                                    type = Rolletype.BARN,
                                ),
                            ),
                    ),
                opprettSakRespons()
                    .copy(
                        saksnummer = Saksnummer("sak2"),
                        roller =
                            listOf(
                                RolleDto(
                                    Personident("bmSak2"),
                                    type = Rolletype.BIDRAGSMOTTAKER,
                                ),
                                RolleDto(
                                    Personident("bpSak2"),
                                    type = Rolletype.BIDRAGSPLIKTIG,
                                ),
                                RolleDto(
                                    testdataSøknadsbarn1.personIdent,
                                    type = Rolletype.BARN,
                                ),
                            ),
                    ),
            )
        every { bidragPersonConsumer.hentPersonHusstandsmedlemmer(eq(testdataBidragsmottaker.personIdent)) } returns
            opprettHusstandsmedlemRespons()
        every { bidragPersonConsumer.hentPersonHusstandsmedlemmer(eq(Personident("bmSak2"))) } returns
            opprettHusstandsmedlemRespons()
        every { bidragPersonConsumer.hentPerson(eq(testdataSøknadsbarn1.personIdent)) } returns testdataSøknadsbarn1.tilPersonDto()
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
        val resultat = service.skalBMFortsattMottaForskuddForSøknadsbarnEtterAdresseendring(personIdentSøknadsbarn1)
        resultat.shouldHaveSize(2)
        assertSoftly(resultat.first()) {
            gjelderBarn shouldBe testdataSøknadsbarn1.personIdent.verdi
            bidragsmottaker shouldBe testdataBidragsmottaker.personIdent.verdi
            saksnummer shouldBe saksnummer
            enhet shouldBe testdataEnhet
        }

        assertSoftly(resultat[1]) {
            gjelderBarn shouldBe testdataSøknadsbarn1.personIdent.verdi
            bidragsmottaker shouldBe "bmSak2"
            saksnummer shouldBe "sak2"
            enhet shouldBe testdataEnhet
        }
    }

    @Test
    fun `skal ikke returnere resultat hvis person ikke er barn i saken`() {
        every { bidragSakConsumer.hentSakerForPerson(eq(testdataSøknadsbarn1.personIdent)) } returns
            listOf(
                opprettSakRespons()
                    .copy(
                        roller =
                            listOf(
                                RolleDto(
                                    testdataBidragsmottaker.personIdent,
                                    type = Rolletype.BIDRAGSMOTTAKER,
                                ),
                                RolleDto(
                                    testdataBidragspliktig.personIdent,
                                    type = Rolletype.BIDRAGSPLIKTIG,
                                ),
                                RolleDto(
                                    testdataSøknadsbarn1.personIdent,
                                    type = Rolletype.BARN,
                                ),
                                RolleDto(
                                    testdataSøknadsbarn2.personIdent,
                                    type = Rolletype.BARN,
                                ),
                            ),
                    ),
                opprettSakRespons()
                    .copy(
                        saksnummer = Saksnummer("sak2"),
                        roller =
                            listOf(
                                RolleDto(
                                    testdataSøknadsbarn1.personIdent,
                                    type = Rolletype.BIDRAGSMOTTAKER,
                                ),
                                RolleDto(
                                    Personident("bpSak2"),
                                    type = Rolletype.BIDRAGSPLIKTIG,
                                ),
                                RolleDto(
                                    testdataSøknadsbarn1.personIdent,
                                    type = Rolletype.REELMOTTAKER,
                                ),
                            ),
                    ),
            )
        every { bidragPersonConsumer.hentPersonHusstandsmedlemmer(eq(testdataBidragsmottaker.personIdent)) } returns
            opprettHusstandsmedlemRespons(testdataSøknadsbarn2.personIdent)
        every { bidragPersonConsumer.hentPersonHusstandsmedlemmer(eq(Personident("bmSak2"))) } returns
            opprettHusstandsmedlemRespons()
        every { bidragPersonConsumer.hentPerson(eq(testdataSøknadsbarn1.personIdent)) } returns testdataSøknadsbarn1.tilPersonDto()
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
        val resultat = service.skalBMFortsattMottaForskuddForSøknadsbarnEtterAdresseendring(personIdentSøknadsbarn1)
        resultat.shouldHaveSize(1)
        resultat.first().saksnummer shouldBe saksnummer
    }

    @Test
    fun `skal ikke returnere resultat hvis barnet fortsatt bor hos BM`() {
        every { bidragPersonConsumer.hentPersonHusstandsmedlemmer(eq(testdataBidragsmottaker.personIdent)) } returns
            opprettHusstandsmedlemRespons(testdataSøknadsbarn1.personIdent, testdataSøknadsbarn2.personIdent)
        every { bidragPersonConsumer.hentPerson(eq(testdataSøknadsbarn1.personIdent)) } returns testdataSøknadsbarn1.tilPersonDto()
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
        val resultat = service.skalBMFortsattMottaForskuddForSøknadsbarnEtterAdresseendring(personIdentSøknadsbarn1)
        resultat.shouldHaveSize(0)
    }

    @Test
    fun `skal ikke returnere resultat hvis barnet ikke har bidrag sak`() {
        every { bidragSakConsumer.hentSakerForPerson(eq(testdataSøknadsbarn1.personIdent)) } returns emptyList()
        every { bidragPersonConsumer.hentPersonHusstandsmedlemmer(eq(testdataBidragsmottaker.personIdent)) } returns
            opprettHusstandsmedlemRespons()
        every { bidragPersonConsumer.hentPerson(eq(testdataSøknadsbarn1.personIdent)) } returns testdataSøknadsbarn1.tilPersonDto()
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns opprettLøpendeForskuddRespons()
        val resultat = service.skalBMFortsattMottaForskuddForSøknadsbarnEtterAdresseendring(personIdentSøknadsbarn1)
        resultat.shouldHaveSize(0)
    }

    @Test
    fun `skal ikke returnere resultat hvis barnet ikke har løpende forskudd`() {
        every { bidragPersonConsumer.hentPersonHusstandsmedlemmer(eq(testdataBidragsmottaker.personIdent)) } returns
            opprettHusstandsmedlemRespons()
        every { bidragPersonConsumer.hentPerson(eq(testdataSøknadsbarn1.personIdent)) } returns testdataSøknadsbarn1.tilPersonDto()
        every { bidragBeløpshistorikkConsumer.hentHistoriskeStønader(any()) } returns
            opprettStønadDto(
                stønadstype = Stønadstype.FORSKUDD,
                periodeListe = emptyList(),
            )
        val resultat = service.skalBMFortsattMottaForskuddForSøknadsbarnEtterAdresseendring(personIdentSøknadsbarn1)
        resultat.shouldHaveSize(0)
    }
}

private fun opprettLøpendeForskuddRespons(
    vedtaksid: Int = vedtaksidForskudd,
    beløp: BigDecimal = BigDecimal("5600"),
) = opprettStønadDto(
    stønadstype = Stønadstype.FORSKUDD,
    periodeListe =
        listOf(
            opprettStønadPeriodeDto(
                ÅrMånedsperiode(LocalDate.now().minusMonths(4), null),
                beløp = beløp,
                vedtakId = vedtaksid,
            ),
        ),
)

private fun opprettHusstandsmedlemRespons(vararg husstandsmedlemmer: Personident) =
    HusstandsmedlemmerDto(
        husstandListe =
            listOf(
                Husstand(
                    gyldigFraOgMed = LocalDate.now(),
                    gyldigTilOgMed = null,
                    husstandsmedlemListe =
                        husstandsmedlemmer.map {
                            Husstandsmedlem(
                                gyldigFraOgMed = LocalDate.now(),
                                gyldigTilOgMed = null,
                                personId = it,
                                navn = "",
                            )
                        },
                ),
            ),
    )
