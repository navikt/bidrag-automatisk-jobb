package no.nav.bidrag.automatiskjobb.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.vedtak.Stønadsendring
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger { }

@Service
class VedtakService(
    private val barnRepository: BarnRepository,
    private val identUtils: IdentUtils,
    private val bidragPersonConsumer: BidragPersonConsumer,
) {
    @Transactional
    fun behandleVedtak(vedtakHendelse: VedtakHendelse) {
        if (vedtakHendelse.type == Vedtakstype.INDEKSREGULERING) return
        val stønadsendringerFraVedtak = hentStønadsendringerForBidragOgForskudd(vedtakHendelse)
        stønadsendringerFraVedtak?.forEach { (kravhaverSak, stønadsendringer) ->
            val (sak, kravhaver) = kravhaverSak
            val kravhaverNyesteIdent = identUtils.hentNyesteIdent(kravhaver)
            val kravhaverAlleIdenter = identUtils.hentAlleIdenter(kravhaver)
            val lagretBarn =
                barnRepository.finnBarnForKravhaverIdenterOgSaksnummer(
                    kravhaverAlleIdenter,
                    sak.verdi,
                )
            if (lagretBarn != null) {
                oppdaterBarn(lagretBarn, stønadsendringer)
            } else {
                opprettBarnFraStønadsendring(kravhaverNyesteIdent, stønadsendringer)
            }
        }
    }

    private fun oppdaterBarn(
        lagretBarn: Barn,
        stønadsendringer: List<Stønadsendring>,
    ) {
        LOGGER.info { "Oppdaterer barn ${lagretBarn.id} for sak ${stønadsendringer.first().sak.verdi}" }
        val oppdatertSkyldner = finnSkyldner(stønadsendringer)

        // Skylder kan oppdateres om det finnes en ny skyldner som ikke er null
        if (!oppdatertSkyldner.isNullOrEmpty()) {
            lagretBarn.skyldner = oppdatertSkyldner
        }

        // Forskudd og bidrag kan oppdateres om det finnes en ny periode for stønadstypen. Her tillates nye verdier å være null.
        if (stønadsendringer.any { it.type == Stønadstype.BIDRAG }) {
            lagretBarn.apply {
                bidragFra = finnPeriodeFra(stønadsendringer, Stønadstype.BIDRAG, bidragFra)
                bidragTil = finnPeriodeTil(stønadsendringer, Stønadstype.BIDRAG, bidragTil)
            }
        }
        if (stønadsendringer.any { it.type == Stønadstype.FORSKUDD }) {
            lagretBarn.apply {
                forskuddFra = finnPeriodeFra(stønadsendringer, Stønadstype.FORSKUDD, forskuddFra)
                forskuddTil = finnPeriodeTil(stønadsendringer, Stønadstype.FORSKUDD, forskuddTil)
            }
        }
        lagretBarn.oppdatert = LocalDateTime.now()
    }

    /**
     * Skal filtrere vekk alle vedtak som ikke er bidrag eller forskudd, som er uten innkreving og som er endring
     */
    private fun hentStønadsendringerForBidragOgForskudd(vedtakHendelse: VedtakHendelse) =
        vedtakHendelse.stønadsendringListe
            ?.filter { it.type == Stønadstype.BIDRAG || it.type == Stønadstype.FORSKUDD }
            ?.filter { it.innkreving == Innkrevingstype.MED_INNKREVING }
            ?.filter { it.beslutning == Beslutningstype.ENDRING }
            ?.groupBy { it.sak to it.kravhaver }

    private fun opprettBarnFraStønadsendring(
        kravhaver: Personident,
        stønadsendringer: List<Stønadsendring>,
    ) {
        val saksnummer = stønadsendringer.first().sak.verdi
        val barn =
            Barn(
                saksnummer = saksnummer,
                kravhaver = kravhaver.verdi,
                fødselsdato = hentFødselsdatoForPerson(kravhaver),
                skyldner = finnSkyldner(stønadsendringer),
                forskuddFra = finnPeriodeFra(stønadsendringer, Stønadstype.FORSKUDD),
                forskuddTil = finnPeriodeTil(stønadsendringer, Stønadstype.FORSKUDD),
                bidragFra = finnPeriodeFra(stønadsendringer, Stønadstype.BIDRAG),
                bidragTil = finnPeriodeTil(stønadsendringer, Stønadstype.BIDRAG),
                oppdatert = LocalDateTime.now(),
            )
        val lagretBarn = barnRepository.save(barn)
        LOGGER.info { "Opprettet nytt barn ${lagretBarn.id} for sak $saksnummer" }
    }

    private fun hentFødselsdatoForPerson(kravhaver: Personident): LocalDate? {
        try {
            return bidragPersonConsumer.hentFødselsdatoForPerson(kravhaver)
        } catch (e: Exception) {
            LOGGER.error(e) { "Det skjedde en feil ved henting av fødselsdato for person $kravhaver" }
            throw e
        }
    }

    private fun finnPeriodeFra(
        stønadsendring: List<Stønadsendring>,
        stønadstype: Stønadstype,
        eksisterendePeriodeFra: LocalDate? = null,
    ): LocalDate? {
        val nyPeriodeFra =
            stønadsendring
                .find { it.type == stønadstype }
                ?.periodeListe
                ?.filter { it.beløp != null }
                ?.map { it.periode.toDatoperiode() }
                ?.minByOrNull { it.fom }
                ?.fom

        return if (eksisterendePeriodeFra != null && nyPeriodeFra != null) {
            minOf(eksisterendePeriodeFra, nyPeriodeFra)
        } else {
            nyPeriodeFra ?: eksisterendePeriodeFra
        }
    }

    private fun finnPeriodeTil(
        stønadsendring: List<Stønadsendring>,
        stønadstype: Stønadstype,
        eksisterendePeriodeTil: LocalDate? = null,
    ): LocalDate? {
        val nyPeriodeTil =
            stønadsendring
                .find { it.type == stønadstype }
                ?.periodeListe
                ?.filter { it.beløp != null }
                ?.map { it.periode.toDatoperiode() }
                ?.takeIf { it.all { periode -> periode.til != null } }
                ?.sortedByDescending { it.til }
                ?.firstOrNull()
                ?.til

        // Om det er avsluttende periode så skal den avsluttende periodens fra dato settes som tilDato da dette er starten på opphøret.
        val avsluttendePeriode =
            stønadsendring
                .filter { it.type == stønadstype }
                .flatMap { it.periodeListe }
                .find { it.beløp == null }
                ?.periode
        if (avsluttendePeriode != null) {
            return avsluttendePeriode.toDatoperiode().fom
        }

        // Velg den som er lengst frem i tid av ny eller eksisterende periode, behold null om det finnes
        return if (eksisterendePeriodeTil != null && nyPeriodeTil != null) {
            maxOf(eksisterendePeriodeTil, nyPeriodeTil)
        } else {
            null
        }
    }

    private fun finnSkyldner(stønadsendring: List<Stønadsendring>): String? =
        stønadsendring
            .find {
                it.type == Stønadstype.BIDRAG
            }?.skyldner
            ?.let { identUtils.hentNyesteIdent(it) }
            ?.verdi
}
