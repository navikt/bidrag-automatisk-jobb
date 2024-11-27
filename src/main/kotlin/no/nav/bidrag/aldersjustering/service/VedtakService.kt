package no.nav.bidrag.aldersjustering.service

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import no.nav.bidrag.aldersjustering.SECURE_LOGGER
import no.nav.bidrag.aldersjustering.consumer.BidragPersonConsumer
import no.nav.bidrag.aldersjustering.persistence.entity.Barn
import no.nav.bidrag.aldersjustering.persistence.repository.BarnRepository
import no.nav.bidrag.aldersjustering.utils.IdentUtils
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.vedtak.Stønadsendring
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VedtakService(
    private val objectMapper: ObjectMapper,
    private val barnRepository: BarnRepository,
    private val identUtils: IdentUtils,
    private val bidragPersonConsumer: BidragPersonConsumer,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(VedtakService::class.java)
    }

    @Transactional
    fun behandleVedtak(hendelse: String) {
        val vedtakHendelse = mapVedtakHendelse(hendelse)

        val stønadsendringer = hentStønadsendringerForBidragOgForskudd(vedtakHendelse)

        stønadsendringer?.forEach { (kravhaver, stønadsendringer) ->
            val kravhaverNyesteIdent = identUtils.hentNyesteIdent(kravhaver)

            val lagretBarn = barnRepository.findByKravhaverAndSaksnummer(
                kravhaverNyesteIdent.verdi,
                stønadsendringer.first().sak.verdi
            )
            if (lagretBarn != null) {
                oppdaterBarn(lagretBarn, stønadsendringer)
            } else {
                opprettBarnFraStønadsendring(kravhaver, stønadsendringer)
            }
        }
    }

    private fun oppdaterBarn(lagretBarn: Barn, stønadsendringer: List<Stønadsendring>) {
        LOGGER.info("Oppdaterer barn ${lagretBarn.id}.")
        val skyldner = finnSkylder(stønadsendringer)

        // Skylder kan oppdateres om det finnes en ny skyldner som ikke er null
        lagretBarn.apply {
            skyldner?.let { this.skyldner = it }
        }

        // Forskudd og bidrag kan oppdateres om det finnes en ny periode for stønadstypen. Her tillates nye verdier å være null.
        if (stønadsendringer.any { it.type == Stønadstype.BIDRAG }) {
            lagretBarn.apply {
                bidragFra = finnPeriodeFra(stønadsendringer, Stønadstype.BIDRAG)
                bidragTil = finnPeriodeTil(stønadsendringer, Stønadstype.BIDRAG)
            }
        }
        if (stønadsendringer.any { it.type == Stønadstype.FORSKUDD }) {
            lagretBarn.apply {
                forskuddFra = finnPeriodeFra(stønadsendringer, Stønadstype.FORSKUDD)
                forskuddTil = finnPeriodeTil(stønadsendringer, Stønadstype.FORSKUDD)
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

    private fun opprettBarnFraStønadsendring(kravhaver: Personident, stønadsendring: List<Stønadsendring>) {
        val saksnummer = stønadsendring.first().sak.verdi
        val barn = Barn(
            saksnummer = saksnummer,
            kravhaver = kravhaver.verdi,
            fødselsdato = bidragPersonConsumer.hentFødselsdatoForPerson(kravhaver),
            skyldner = finnSkylder(stønadsendring),
            forskuddFra = finnPeriodeFra(stønadsendring, Stønadstype.FORSKUDD),
            forskuddTil = finnPeriodeTil(stønadsendring, Stønadstype.FORSKUDD),
            bidragFra = finnPeriodeFra(stønadsendring, Stønadstype.BIDRAG),
            bidragTil = finnPeriodeTil(stønadsendring, Stønadstype.BIDRAG),
        )
        barnRepository.save(barn)
    }

    private fun finnPeriodeFra(stønadsendring: List<Stønadsendring>, stønadstype: Stønadstype): LocalDate? {
        return stønadsendring.find { it.type == stønadstype }
            ?.periodeListe
            ?.map { it.periode.toDatoperiode() }
            ?.minByOrNull { it.fom }
            ?.fom
    }

    private fun finnPeriodeTil(stønadsendring: List<Stønadsendring>, stønadstype: Stønadstype): LocalDate? {
        return stønadsendring.find { it.type == stønadstype }
            ?.periodeListe
            ?.map { it.periode.toDatoperiode() }
            ?.takeIf { it.all { periode -> periode.til != null } }
            ?.sortedByDescending { it.til }
            ?.firstOrNull()
            ?.til
    }

    private fun finnSkylder(stønadsendring: List<Stønadsendring>): String? {
        return stønadsendring.find { it.type == Stønadstype.BIDRAG }?.skyldner?.let { identUtils.hentNyesteIdent(it) }?.verdi
    }


    private fun mapVedtakHendelse(hendelse: String): VedtakHendelse =
        try {
            objectMapper.readValue(hendelse, VedtakHendelse::class.java)
        } finally {
            SECURE_LOGGER.debug("Leser hendelse: {}", hendelse)
        }
}
