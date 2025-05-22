package no.nav.bidrag.automatiskjobb.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.vedtak.Periode
import no.nav.bidrag.transport.behandling.vedtak.Sporingsdata
import no.nav.bidrag.transport.behandling.vedtak.Stønadsendring
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.behandling.vedtak.saksnummer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
@Transactional
class VedtakServiceTest {
    @RelaxedMockK
    private lateinit var barnRepository: BarnRepository

    @RelaxedMockK
    private lateinit var objectMapper: ObjectMapper

    @RelaxedMockK
    private lateinit var identUtils: IdentUtils

    @RelaxedMockK
    private lateinit var bidragPersonConsumer: BidragPersonConsumer

    @InjectMockKs
    private lateinit var vedtakService: VedtakService

    private val barnSlot = slot<Barn>()

    @BeforeEach
    fun setup() {
        every { identUtils.hentNyesteIdent(any()) } returnsArgument 0
    }

    @Test
    fun skalOppretteNyttBarnForBidrag(): Unit =
        runBlocking {
            val vedtakHendelse = opprettVedtakHendelse(Stønadstype.BIDRAG, Innkrevingstype.MED_INNKREVING)

            mocks(vedtakHendelse)

            vedtakService.behandleVedtak(vedtakHendelse)

            verify(exactly = 1) { barnRepository.save(any()) }
            barnSlot.captured shouldNotBe null
            barnSlot.captured.saksnummer shouldBe vedtakHendelse.saksnummer
            barnSlot.captured.skyldner shouldBe
                vedtakHendelse.stønadsendringListe
                    ?.first()
                    ?.skyldner
                    ?.verdi
            barnSlot.captured.kravhaver shouldBe
                vedtakHendelse.stønadsendringListe
                    ?.first()
                    ?.kravhaver
                    ?.verdi
            barnSlot.captured.bidragFra shouldBe
                vedtakHendelse.stønadsendringListe
                    ?.first()
                    ?.periodeListe
                    ?.first()
                    ?.periode
                    ?.toDatoperiode()
                    ?.fom
            barnSlot.captured.bidragTil shouldBe
                vedtakHendelse.stønadsendringListe
                    ?.first()
                    ?.periodeListe
                    ?.first()
                    ?.periode
                    ?.toDatoperiode()
                    ?.til
            barnSlot.captured.forskuddFra shouldBe null
            barnSlot.captured.forskuddTil shouldBe null
        }

    @Test
    fun skalOppretteNyttBarnForFlereSaker(): Unit =
        runBlocking {
            val kravhaver = Personident(PersonidentGenerator.genererFødselsnummer())
            val vedtakHendelse =
                opprettVedtakHendelse(
                    Stønadstype.BIDRAG,
                    Innkrevingstype.MED_INNKREVING,
                    stønadsendringListe =
                        listOf(
                            Stønadsendring(
                                type = Stønadstype.BIDRAG,
                                sak = Saksnummer("112323"),
                                skyldner = Personident(PersonidentGenerator.genererFødselsnummer()),
                                kravhaver = kravhaver,
                                mottaker = Personident(PersonidentGenerator.genererFødselsnummer()),
                                innkreving = Innkrevingstype.MED_INNKREVING,
                                beslutning = Beslutningstype.ENDRING,
                                periodeListe =
                                    listOf(
                                        Periode(
                                            periode = ÅrMånedsperiode(LocalDate.now().minusMonths(2).withDayOfMonth(1), null),
                                            beløp = BigDecimal.valueOf(1000),
                                            valutakode = "NOK",
                                            resultatkode = "OK",
                                            delytelseId = null,
                                        ),
                                    ),
                                førsteIndeksreguleringsår = null,
                                omgjørVedtakId = null,
                                eksternReferanse = null,
                            ),
                            Stønadsendring(
                                type = Stønadstype.FORSKUDD,
                                sak = Saksnummer("123"),
                                skyldner = Personident(PersonidentGenerator.genererFødselsnummer()),
                                kravhaver = kravhaver,
                                mottaker = Personident(PersonidentGenerator.genererFødselsnummer()),
                                innkreving = Innkrevingstype.MED_INNKREVING,
                                beslutning = Beslutningstype.ENDRING,
                                periodeListe =
                                    listOf(
                                        Periode(
                                            periode = ÅrMånedsperiode(LocalDate.now().minusMonths(1).withDayOfMonth(1), null),
                                            beløp = BigDecimal.valueOf(1000),
                                            valutakode = "NOK",
                                            resultatkode = "OK",
                                            delytelseId = null,
                                        ),
                                    ),
                                førsteIndeksreguleringsår = null,
                                omgjørVedtakId = null,
                                eksternReferanse = null,
                            ),
                        ),
                )

            mocks(vedtakHendelse)

            vedtakService.behandleVedtak(vedtakHendelse)

            verify(exactly = 1) {
                barnRepository.save(
                    withArg {
                        it.saksnummer shouldBe "123"
                    },
                )
            }

            verify(exactly = 1) {
                barnRepository.save(
                    withArg {
                        it.saksnummer shouldBe "112323"
                    },
                )
            }
        }

    @Test
    fun skalOppretteNyttBarnForBidragOgForskudd(): Unit =
        runBlocking {
            val kravhaver = Personident(PersonidentGenerator.genererFødselsnummer())
            val vedtakHendelse =
                opprettVedtakHendelse(
                    Stønadstype.BIDRAG,
                    Innkrevingstype.MED_INNKREVING,
                    stønadsendringListe =
                        listOf(
                            Stønadsendring(
                                type = Stønadstype.BIDRAG,
                                sak = Saksnummer("123"),
                                skyldner = Personident(PersonidentGenerator.genererFødselsnummer()),
                                kravhaver = kravhaver,
                                mottaker = Personident(PersonidentGenerator.genererFødselsnummer()),
                                innkreving = Innkrevingstype.MED_INNKREVING,
                                beslutning = Beslutningstype.ENDRING,
                                periodeListe =
                                    listOf(
                                        Periode(
                                            periode = ÅrMånedsperiode(LocalDate.now().minusMonths(2).withDayOfMonth(1), null),
                                            beløp = BigDecimal.valueOf(1000),
                                            valutakode = "NOK",
                                            resultatkode = "OK",
                                            delytelseId = null,
                                        ),
                                    ),
                                førsteIndeksreguleringsår = null,
                                omgjørVedtakId = null,
                                eksternReferanse = null,
                            ),
                            Stønadsendring(
                                type = Stønadstype.FORSKUDD,
                                sak = Saksnummer("123"),
                                skyldner = Personident(PersonidentGenerator.genererFødselsnummer()),
                                kravhaver = kravhaver,
                                mottaker = Personident(PersonidentGenerator.genererFødselsnummer()),
                                innkreving = Innkrevingstype.MED_INNKREVING,
                                beslutning = Beslutningstype.ENDRING,
                                periodeListe =
                                    listOf(
                                        Periode(
                                            periode = ÅrMånedsperiode(LocalDate.now().minusMonths(1).withDayOfMonth(1), null),
                                            beløp = BigDecimal.valueOf(1000),
                                            valutakode = "NOK",
                                            resultatkode = "OK",
                                            delytelseId = null,
                                        ),
                                    ),
                                førsteIndeksreguleringsår = null,
                                omgjørVedtakId = null,
                                eksternReferanse = null,
                            ),
                        ),
                )

            mocks(vedtakHendelse)

            vedtakService.behandleVedtak(vedtakHendelse)

            verify(exactly = 1) { barnRepository.save(any()) }
            barnSlot.captured shouldNotBe null
            barnSlot.captured.saksnummer shouldBe vedtakHendelse.saksnummer
            barnSlot.captured.skyldner shouldBe
                vedtakHendelse.stønadsendringListe
                    ?.first()
                    ?.skyldner
                    ?.verdi
            barnSlot.captured.kravhaver shouldBe
                vedtakHendelse.stønadsendringListe
                    ?.first()
                    ?.kravhaver
                    ?.verdi
            barnSlot.captured.bidragFra shouldBe
                vedtakHendelse.stønadsendringListe
                    ?.first()
                    ?.periodeListe
                    ?.first()
                    ?.periode
                    ?.toDatoperiode()
                    ?.fom
            barnSlot.captured.bidragTil shouldBe
                vedtakHendelse.stønadsendringListe
                    ?.first()
                    ?.periodeListe
                    ?.first()
                    ?.periode
                    ?.toDatoperiode()
                    ?.til
            barnSlot.captured.forskuddFra shouldBe
                vedtakHendelse.stønadsendringListe
                    ?.last()
                    ?.periodeListe
                    ?.first()
                    ?.periode
                    ?.toDatoperiode()
                    ?.fom
            barnSlot.captured.forskuddTil shouldBe
                vedtakHendelse.stønadsendringListe
                    ?.last()
                    ?.periodeListe
                    ?.first()
                    ?.periode
                    ?.toDatoperiode()
                    ?.til
        }

    @Test
    fun skalOppretteToBarnOmVedtakInnholderToKravhavere(): Unit =
        runBlocking {
            val vedtakHendelse =
                opprettVedtakHendelse(
                    Stønadstype.BIDRAG,
                    Innkrevingstype.MED_INNKREVING,
                    stønadsendringListe =
                        listOf(
                            Stønadsendring(
                                type = Stønadstype.BIDRAG,
                                sak = Saksnummer("123"),
                                skyldner = Personident(PersonidentGenerator.genererFødselsnummer()),
                                kravhaver =
                                    Personident(
                                        PersonidentGenerator.genererFødselsnummer(
                                            innsendtFodselsdato = LocalDate.now().minusYears(10),
                                        ),
                                    ),
                                mottaker = Personident(PersonidentGenerator.genererFødselsnummer()),
                                innkreving = Innkrevingstype.MED_INNKREVING,
                                beslutning = Beslutningstype.ENDRING,
                                periodeListe =
                                    listOf(
                                        Periode(
                                            periode = ÅrMånedsperiode(LocalDate.now().minusMonths(2).withDayOfMonth(1), null),
                                            beløp = BigDecimal.valueOf(1000),
                                            valutakode = "NOK",
                                            resultatkode = "OK",
                                            delytelseId = null,
                                        ),
                                    ),
                                førsteIndeksreguleringsår = null,
                                omgjørVedtakId = null,
                                eksternReferanse = null,
                            ),
                            Stønadsendring(
                                type = Stønadstype.BIDRAG,
                                sak = Saksnummer("123"),
                                skyldner = Personident(PersonidentGenerator.genererFødselsnummer()),
                                kravhaver =
                                    Personident(
                                        PersonidentGenerator.genererFødselsnummer(
                                            innsendtFodselsdato = LocalDate.now().minusYears(12),
                                        ),
                                    ),
                                mottaker = Personident(PersonidentGenerator.genererFødselsnummer()),
                                innkreving = Innkrevingstype.MED_INNKREVING,
                                beslutning = Beslutningstype.ENDRING,
                                periodeListe =
                                    listOf(
                                        Periode(
                                            periode = ÅrMånedsperiode(LocalDate.now().minusMonths(1).withDayOfMonth(1), null),
                                            beløp = BigDecimal.valueOf(1000),
                                            valutakode = "NOK",
                                            resultatkode = "OK",
                                            delytelseId = null,
                                        ),
                                    ),
                                førsteIndeksreguleringsår = null,
                                omgjørVedtakId = null,
                                eksternReferanse = null,
                            ),
                        ),
                )

            mocks(vedtakHendelse)

            vedtakService.behandleVedtak(vedtakHendelse)

            verify(exactly = 2) { barnRepository.save(any()) }
        }

    @Test
    fun vedtakUtenInnkrevingSkalIkkeOppretteBarn(): Unit =
        runBlocking {
            val vedtakHendelse = opprettVedtakHendelse(Stønadstype.BIDRAG, Innkrevingstype.UTEN_INNKREVING)

            mocks(vedtakHendelse)

            vedtakService.behandleVedtak(vedtakHendelse)

            verify(exactly = 0) { barnRepository.save(any()) }
        }

    @Test
    fun skalOppdatereEksisterendeBarn(): Unit =
        runBlocking {
            val kravhaver = Personident(PersonidentGenerator.genererFødselsnummer())
            val eksisterendeBarn =
                Barn(
                    saksnummer = "123",
                    kravhaver = kravhaver.verdi,
                    fødselsdato = LocalDate.now().minusYears(10),
                    skyldner = "123",
                    forskuddFra = LocalDate.now().minusMonths(2),
                    forskuddTil = LocalDate.now().minusMonths(1),
                    bidragFra = LocalDate.now().minusMonths(8),
                    bidragTil = LocalDate.now().minusMonths(5),
                )

            val vedtakHendelse =
                opprettVedtakHendelse(Stønadstype.BIDRAG, Innkrevingstype.MED_INNKREVING, kravhaver = kravhaver)

            mocks(vedtakHendelse, eksisterendeBarn)

            vedtakService.behandleVedtak(vedtakHendelse)

            verify(exactly = 0) { barnRepository.save(any()) }
            eksisterendeBarn shouldNotBe null
            eksisterendeBarn.saksnummer shouldBe vedtakHendelse.saksnummer
            eksisterendeBarn.skyldner shouldBe
                vedtakHendelse.stønadsendringListe
                    ?.first()
                    ?.skyldner
                    ?.verdi
            eksisterendeBarn.kravhaver shouldBe
                vedtakHendelse.stønadsendringListe
                    ?.first()
                    ?.kravhaver
                    ?.verdi
            eksisterendeBarn.bidragFra shouldBe eksisterendeBarn.bidragFra
            eksisterendeBarn.bidragTil shouldBe
                vedtakHendelse.stønadsendringListe
                    ?.first()
                    ?.periodeListe
                    ?.first()
                    ?.periode
                    ?.toDatoperiode()
                    ?.til
            eksisterendeBarn.forskuddFra shouldBe eksisterendeBarn.forskuddFra
            eksisterendeBarn.forskuddTil shouldBe eksisterendeBarn.forskuddTil
        }

    @Test
    fun skalOpphøreLøpendeBidragPåBarnNårBeløpErNull(): Unit =
        runBlocking {
            val kravhaver = Personident(PersonidentGenerator.genererFødselsnummer())
            val eksisterendeBidragFra = LocalDate.now().minusMonths(5)
            val eksisterendeForskuddFra = LocalDate.now().minusMonths(2)
            val eksisterendeForskuddTil = LocalDate.now().minusMonths(1)
            val eksisterendeBarn =
                Barn(
                    saksnummer = "123",
                    kravhaver = kravhaver.verdi,
                    fødselsdato = LocalDate.now().minusYears(10),
                    skyldner = "123",
                    forskuddFra = eksisterendeForskuddFra,
                    forskuddTil = eksisterendeForskuddTil,
                    bidragFra = eksisterendeBidragFra,
                    bidragTil = null,
                )

            val nyttBidragFra = LocalDate.now().minusMonths(4).withDayOfMonth(1)
            val vedtakHendelse =
                opprettVedtakHendelse(
                    Stønadstype.BIDRAG,
                    Innkrevingstype.MED_INNKREVING,
                    kravhaver = kravhaver,
                    stønadsendringListe =
                        listOf(
                            Stønadsendring(
                                type = Stønadstype.BIDRAG,
                                sak = Saksnummer("123"),
                                skyldner = Personident(PersonidentGenerator.genererFødselsnummer()),
                                kravhaver = kravhaver,
                                mottaker = Personident(PersonidentGenerator.genererFødselsnummer()),
                                innkreving = Innkrevingstype.MED_INNKREVING,
                                beslutning = Beslutningstype.ENDRING,
                                periodeListe =
                                    listOf(
                                        Periode(
                                            periode = ÅrMånedsperiode(nyttBidragFra, null),
                                            beløp = null,
                                            valutakode = null,
                                            resultatkode = "OK",
                                            delytelseId = null,
                                        ),
                                    ),
                                førsteIndeksreguleringsår = null,
                                omgjørVedtakId = null,
                                eksternReferanse = null,
                            ),
                        ),
                )

            mocks(vedtakHendelse, eksisterendeBarn)

            vedtakService.behandleVedtak(vedtakHendelse)

            verify(exactly = 0) { barnRepository.save(any()) }
            eksisterendeBarn shouldNotBe null
            eksisterendeBarn.saksnummer shouldBe vedtakHendelse.saksnummer
            eksisterendeBarn.skyldner shouldBe
                vedtakHendelse.stønadsendringListe
                    ?.first()
                    ?.skyldner
                    ?.verdi
            eksisterendeBarn.kravhaver shouldBe kravhaver.verdi
            eksisterendeBarn.bidragFra shouldBe eksisterendeBidragFra
            eksisterendeBarn.bidragTil shouldBe nyttBidragFra
            eksisterendeBarn.forskuddFra shouldBe eksisterendeForskuddFra
            eksisterendeBarn.forskuddTil shouldBe eksisterendeForskuddTil
        }

    private fun mocks(
        vedtakHendelse: VedtakHendelse,
        eksisterendeBarn: Barn? = null,
    ) {
        every { objectMapper.readValue(any<String>(), VedtakHendelse::class.java) } returns vedtakHendelse
        every { bidragPersonConsumer.hentFødselsdatoForPerson(any()) } returns LocalDate.now().minusYears(8)
        every { barnRepository.findByKravhaverAndSaksnummer(any(), any()) } returns eksisterendeBarn
        every { barnRepository.finnBarnForKravhaverIdenterOgSaksnummer(any(), any()) } returns eksisterendeBarn
        every { barnRepository.save(capture(barnSlot)) } answers { firstArg() }
    }

    private fun opprettVedtakHendelse(
        stønadstype: Stønadstype,
        innkreving: Innkrevingstype,
        skyldner: Personident = Personident(PersonidentGenerator.genererFødselsnummer()),
        kravhaver: Personident = Personident(PersonidentGenerator.genererFødselsnummer()),
        stønadsendringListe: List<Stønadsendring> =
            listOf(
                Stønadsendring(
                    type = stønadstype,
                    sak = Saksnummer("123"),
                    skyldner = skyldner,
                    kravhaver = kravhaver,
                    mottaker = Personident(PersonidentGenerator.genererFødselsnummer()),
                    innkreving = innkreving,
                    beslutning = Beslutningstype.ENDRING,
                    periodeListe =
                        listOf(
                            Periode(
                                periode = ÅrMånedsperiode(LocalDate.now().minusMonths(4).withDayOfMonth(1), null),
                                beløp = BigDecimal.valueOf(1000),
                                valutakode = "NOK",
                                resultatkode = "OK",
                                delytelseId = null,
                            ),
                        ),
                    førsteIndeksreguleringsår = null,
                    omgjørVedtakId = null,
                    eksternReferanse = null,
                ),
            ),
    ) = VedtakHendelse(
        kilde = Vedtakskilde.MANUELT,
        type = Vedtakstype.FASTSETTELSE,
        id = 1,
        opprettetAv = "Simen",
        kildeapplikasjon = "Aldersjustering",
        vedtakstidspunkt = LocalDateTime.now(),
        opprettetTidspunkt = LocalDateTime.now(),
        opprettetAvNavn = "Simen",
        enhetsnummer = null,
        innkrevingUtsattTilDato = null,
        fastsattILand = null,
        stønadsendringListe = stønadsendringListe,
        engangsbeløpListe = emptyList(),
        behandlingsreferanseListe = emptyList(),
        sporingsdata = Sporingsdata(correlationId = "123"),
    )
}
