package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.springframework.stereotype.Service
import java.math.BigDecimal

private val LOGGER = KotlinLogging.logger { }

/** Landkode for Norge – linjer med denne koden havner i bidragsreskontro-rapporten (type 1). */
const val LANDKODE_NORGE = "NO"

/**
 * Én rad i en indeksregulering-rapport. Tilsvarer `BatchFb020RappDO` i bisys.
 *
 * @property fnrBp fødselsnummer til bidragspliktig (skyldner)
 * @property fnrBa fødselsnummer til barnet/kravhaver
 */
data class RapportLinje(
    val saksnummer: String,
    val fnrBp: String,
    val fnrBa: String,
    val beløp: BigDecimal,
    val landkode: String,
)

/**
 * Rapportdata gruppert per rapporttype, tilsvarende de fem rapportstegene i bisys `FB020Config`.
 */
data class RapporterIndeksreguleringBidragData(
    val bidragsreskontro: List<RapportLinje> = emptyList(),
    val bpUtlandBrev: List<RapportLinje> = emptyList(),
    val bpUtlandDiskresjon: List<RapportLinje> = emptyList(),
    val bpUtlandManglerAdresse: List<RapportLinje> = emptyList(),
    val elin: List<RapportLinje> = emptyList(),
)

/**
 * Bygger rapportdata for indeksregulering av bidrag, som mater rapportstegene i
 * [no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.rapporter]. Gjenskaper datagrunnlaget
 * som i bisys ble samlet i en temporær rapport-tabell under selve indeksreguleringen.
 *
 * Leser alle gjennomførte [Indeksregulering]-rader for [Stønadstype.BIDRAG] og gitt år, og bygger
 * én [RapportLinje] per barn.
 *
 * 🔴 RØD SONE – må implementeres av domeneeier før rapportene kan brukes:
 *  - [hentBeløp]: indeksjustert beløp produseres først når
 *    [IndeksreguleringBidragService.gjennomforBidrag] er implementert og beløpet persisteres.
 *  - [klassifiser]: skille Norge (type 1) fra BP i utlandet (type 2–4) basert på landkode,
 *    diskresjon og manglende adresse. Krever oppslag mot person-/adressedata.
 */
@Service
class RapporterIndeksreguleringBidragService(
    private val indeksreguleringRepository: IndeksreguleringRepository,
) {
    fun byggRapportData(år: Int): RapporterIndeksreguleringBidragData {
        val reguleringer = indeksreguleringRepository.findAllByGjennomfortTrueAndStønadstypeAndÅr(Stønadstype.BIDRAG, år)
        LOGGER.info { "Bygger rapportdata for ${reguleringer.size} gjennomførte indeksregulering-saker (bidrag) for år $år." }

        val linjer = reguleringer.flatMap { regulering -> regulering.barn.mapNotNull { barn -> tilRapportLinje(regulering, barn) } }
        return klassifiser(linjer)
    }

    private fun tilRapportLinje(
        regulering: Indeksregulering,
        barn: Barn,
    ): RapportLinje? {
        val skyldner = barn.skyldner
        if (skyldner.isNullOrBlank()) {
            LOGGER.warn { "Hopper over barn ${barn.id} i sak ${regulering.saksnummer} – mangler skyldner (bidragspliktig)." }
            return null
        }
        return RapportLinje(
            saksnummer = regulering.saksnummer,
            fnrBp = skyldner,
            fnrBa = barn.kravhaver,
            beløp = hentBeløp(regulering, barn),
            landkode = LANDKODE_NORGE,
        )
    }

    /**
     * 🔴 RØD SONE. Henter det indeksjusterte beløpet for en gitt sak/barn.
     *
     * TODO: Hent beløpet fra det gjennomførte indeksregulerings-vedtaket (eller persistert beløp)
     *  når [IndeksreguleringBidragService.gjennomforBidrag] er ferdig implementert. Inntil da
     *  returneres 0 og det logges en advarsel, slik at filene ikke brukes med feil beløp.
     */
    private fun hentBeløp(
        regulering: Indeksregulering,
        barn: Barn,
    ): BigDecimal {
        LOGGER.warn {
            "Beløp for indeksregulering-rapport er ikke implementert (RØD SONE). " +
                "Returnerer 0 for sak ${regulering.saksnummer}, barn ${barn.id}."
        }
        return BigDecimal.ZERO
    }

    /**
     * 🔴 RØD SONE. Klassifiserer linjene i de fem rapporttypene.
     *
     * TODO: Skill Norge (type 1) fra BP i utlandet (type 2–4) basert på landkode, diskresjon og
     *  manglende adresseinformasjon, via oppslag mot person-/adressedata. Inntil dette er
     *  implementert behandles alle linjer som norske (type 1) og inngår i Elin-rapporten (type 5).
     */
    private fun klassifiser(linjer: List<RapportLinje>): RapporterIndeksreguleringBidragData =
        RapporterIndeksreguleringBidragData(
            bidragsreskontro = linjer.filter { it.landkode == LANDKODE_NORGE },
            elin = linjer,
        )
}
