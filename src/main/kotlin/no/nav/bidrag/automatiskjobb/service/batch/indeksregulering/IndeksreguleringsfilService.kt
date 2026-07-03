package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.consumer.BidragPersonConsumer
import no.nav.bidrag.automatiskjobb.persistence.bucket.ByteArrayOutputStreamTilByteBuffer
import no.nav.bidrag.automatiskjobb.persistence.bucket.GcpFilBucket
import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.entity.Indeksregulering
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.automatiskjobb.persistence.repository.IndeksreguleringRepository
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.land.Landkode2
import no.nav.bidrag.transport.person.PersondetaljerDto
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val LOGGER = KotlinLogging.logger { }

@Service
class IndeksreguleringsfilService(
    private val indeksreguleringRepository: IndeksreguleringRepository,
    private val bidragPersonConsumer: BidragPersonConsumer,
    private val gcpFilBucket: GcpFilBucket,
) {
    companion object {
        val TIDSFORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val LANDKODE_NORGE = Landkode2("NO")
    }

    fun lastOppFil(
        mappe: String,
        filnavnRot: String,
        indeksdato: LocalDate,
        innhold: String,
    ): Boolean {
        val filnavn = "$mappe$filnavnRot-${indeksdato.format(TIDSFORMAT)}.txt"
        val buffer = ByteArrayOutputStreamTilByteBuffer().apply { write(innhold.toByteArray(Charsets.UTF_8)) }
        gcpFilBucket.lagreFil(filnavn, buffer, contentType = "text/plain")
        return true
    }

    fun byggRapportData(år: Int): RapporterIndeksreguleringBidragData {
        val reguleringer =
            indeksreguleringRepository.findAllByStatusAndBehandlingstypeAndStønadstypeInAndÅr(
                Status.FATTET,
                Behandlingstype.FATTET_FORSLAG,
                listOf(Stønadstype.BIDRAG, Stønadstype.OPPFOSTRINGSBIDRAG, Stønadstype.BIDRAG18AAR),
                år,
            )
        LOGGER.info { "Bygger rapportdata for ${reguleringer.size} gjennomførte indeksregulerte bidragssaker  for år $år." }

        val linjer = reguleringer.mapNotNull { regulering -> tilRapportLinje(regulering, regulering.barn) }
        return klassifiser(linjer)
    }

    private fun tilRapportLinje(
        indeksregulering: Indeksregulering,
        barn: Barn,
    ): RapportLinje? {
        val skyldner = barn.skyldner
        if (skyldner.isNullOrBlank()) {
            LOGGER.warn { "Hopper over barn ${barn.id} i sak ${indeksregulering.barn.saksnummer} – mangler skyldner." }
            return null
        }
        if (indeksregulering.beløp == null) {
            LOGGER.error { "Hopper over barn ${barn.id} i sak ${indeksregulering.barn.saksnummer} – mangler beløp." }
            return null
        }
        return RapportLinje(
            saksnummer = indeksregulering.barn.saksnummer,
            fnrBp = skyldner,
            fnrBa = barn.kravhaver,
            beløp = indeksregulering.beløp!!,
        )
    }

    /**
     * Klassifiserer rapportlinjer basert på skyldners (BP) landkode, diskresjonskode og adresseinformasjon.
     *
     * - Skyldnere med landkode == NO havner i norsk rapport.
     * - Skyldnere med annen landkode enn NO havner i bpUtlandBrev.
     *   - Av disse: skyldnere med diskresjonskode satt havner i bpUtlandDiskresjon.
     *   - Av disse: skyldnere uten registrert adresse, havner i bpUtlandManglerAdresse.
     * - Alle linjer havner uansett i elin-rapporten.
     */
    private fun klassifiser(linjer: List<RapportLinje>): RapporterIndeksreguleringBidragData {
        val persondetaljerPerFnrBp =
            linjer
                .map { it.fnrBp }
                .distinct()
                .associateWith { bidragPersonConsumer.hentPersondetaljer(Personident(it)) }

        fun persondetaljer(linje: RapportLinje): PersondetaljerDto? = persondetaljerPerFnrBp[linje.fnrBp]

        // Beriker linjene med skyldners landkode (alpha-2), slik at bl.a. bpUtland-rapportene kan vise riktig land.
        val beriketLinjer =
            linjer.map { linje ->
                linje.copy(landkode = persondetaljer(linje)?.adresse?.land?.verdi)
            }

        fun adresseINorge(linje: RapportLinje): Boolean = persondetaljer(linje)?.adresse?.land == LANDKODE_NORGE

        fun harDiskresjonskode(linje: RapportLinje): Boolean = persondetaljer(linje)?.person?.diskresjonskode != null

        fun manglerAdresse(linje: RapportLinje): Boolean =
            persondetaljer(linje)?.adresse == null ||
                (
                    persondetaljer(linje)?.adresse?.adresselinje1 == null &&
                        persondetaljer(linje)?.adresse?.adresselinje2 == null &&
                        persondetaljer(linje)?.adresse?.adresselinje3 == null
                )

        return RapporterIndeksreguleringBidragData(
            bidragsreskontro = beriketLinjer.filter { adresseINorge(it) },
            bpUtlandBrev = beriketLinjer.filter { !adresseINorge(it) },
            bpUtlandDiskresjon = beriketLinjer.filter { !adresseINorge(it) && harDiskresjonskode(it) },
            bpUtlandManglerAdresse = beriketLinjer.filter { !adresseINorge(it) && manglerAdresse(it) },
            elin = beriketLinjer,
        )
    }
}
