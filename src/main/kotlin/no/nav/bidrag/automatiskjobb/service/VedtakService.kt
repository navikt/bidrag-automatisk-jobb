package no.nav.bidrag.automatiskjobb.service

import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import no.nav.bidrag.automatiskjobb.utils.IdentUtils
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.vedtak.Stønadsendring
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class VedtakService(
    private val barnRepository: BarnRepository,
    private val identUtils: IdentUtils,
    private val bidragPersonConsumer: BidragPersonConsumer,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(VedtakService::class.java)
    }

    @Transactional
    fun behandleVedtak(vedtakHendelse: VedtakHendelse) {
        val stønadsendringer = hentStønadsendringerForBidragOgForskudd(vedtakHendelse)

        stønadsendringer?.forEach { (kravhaver, stønadsendringer) ->
            val kravhaverNyesteIdent = identUtils.hentNyesteIdent(kravhaver)

            val lagretBarn =
                barnRepository.findByKravhaverAndSaksnummer(
                    kravhaverNyesteIdent.verdi,
                    stønadsendringer.first().sak.verdi,
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
        LOGGER.debug("Oppdaterer barn ${lagretBarn.id} for sak ${stønadsendringer.first().sak.verdi}")
        val skyldner = finnSkylder(stønadsendringer)

        // Skylder kan oppdateres om det finnes en ny skyldner som ikke er null
        lagretBarn.apply {
            skyldner?.let { this.skyldner = it }
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

        barnRepository.save(lagretBarn)
    }

    /**
     * Skal filtrere vekk alle vedtak som ikke er bidrag eller forskudd og som er uten innkreving
     */
    private fun hentStønadsendringerForBidragOgForskudd(vedtakHendelse: VedtakHendelse) =
        vedtakHendelse.stønadsendringListe
            ?.filter { it.type == Stønadstype.BIDRAG || it.type == Stønadstype.FORSKUDD }
            ?.filter { it.innkreving == Innkrevingstype.MED_INNKREVING }
            ?.groupBy { it.kravhaver }

    private fun opprettBarnFraStønadsendring(
        kravhaver: Personident,
        stønadsendringer: List<Stønadsendring>,
    ) {
        val saksnummer = stønadsendringer.first().sak.verdi
        val barn =
            Barn(
                saksnummer = saksnummer,
                kravhaver = kravhaver.verdi,
                fødselsdato = bidragPersonConsumer.hentFødselsdatoForPerson(kravhaver),
                skyldner = finnSkylder(stønadsendringer),
                forskuddFra = finnPeriodeFra(stønadsendringer, Stønadstype.FORSKUDD),
                forskuddTil = finnPeriodeTil(stønadsendringer, Stønadstype.FORSKUDD),
                bidragFra = finnPeriodeFra(stønadsendringer, Stønadstype.BIDRAG),
                bidragTil = finnPeriodeTil(stønadsendringer, Stønadstype.BIDRAG),
            )
        val lagretBarn = barnRepository.save(barn)
        LOGGER.debug("Opprettet nytt barn ${lagretBarn.id} for sak $saksnummer")
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
                ?.map { it.periode.toDatoperiode() }
                ?.minByOrNull { it.fom }
                ?.fom

        return if (eksisterendePeriodeFra != null && nyPeriodeFra != null) {
            LocalDate.ofEpochDay(minOf(eksisterendePeriodeFra.toEpochDay(), nyPeriodeFra.toEpochDay()))
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
                ?.map { it.periode.toDatoperiode() }
                ?.takeIf { it.all { periode -> periode.til != null } }
                ?.sortedByDescending { it.til }
                ?.firstOrNull()
                ?.til

        // Om det er avsluttende periode så skal den avsluttende periodens fra dato settes som tilDato da dette er starten på opphøret.
        val avsluttendePeriode = stønadsendring.flatMap { it.periodeListe }.find { it.beløp == null }?.periode
        if (avsluttendePeriode != null) {
            return avsluttendePeriode.toDatoperiode().fom
        }

        // Velg den som er lengst frem i tid av ny eller eksisterende periode, behold null om det finnes
        return if (eksisterendePeriodeTil != null && nyPeriodeTil != null) {
            LocalDate.ofEpochDay(maxOf(eksisterendePeriodeTil.toEpochDay(), nyPeriodeTil.toEpochDay()))
        } else {
            null
        }
    }

    private fun finnSkylder(stønadsendring: List<Stønadsendring>): String? =
        stønadsendring
            .find {
                it.type == Stønadstype.BIDRAG
            }?.skyldner
            ?.let { identUtils.hentNyesteIdent(it) }
            ?.verdi
}
