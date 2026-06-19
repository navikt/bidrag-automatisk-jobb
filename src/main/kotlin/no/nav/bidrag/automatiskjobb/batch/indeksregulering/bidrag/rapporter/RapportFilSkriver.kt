package no.nav.bidrag.automatiskjobb.batch.indeksregulering.bidrag.rapporter

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.automatiskjobb.persistence.bucket.ByteArrayOutputStreamTilByteBuffer
import no.nav.bidrag.automatiskjobb.persistence.bucket.GcpFilBucket
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val LOGGER = KotlinLogging.logger { }

/**
 * Skriver rapportfilene som gjenskaper FB020-rapportstegene fra bisys ved å bygge innholdet i minne
 * og streame det til GCP-bucket via [GcpFilBucket] – samme mønster som `AvstemmingsfilGenerator` i
 * bidrag-regnskap.
 *
 * - Objektnavn bygges som `<mappe><filnavnRot>-<indeksdato yyyyMMdd>.txt`.
 * - Hvis mappe eller filnavn ikke er konfigurert, hoppes skrivingen over (jf. bisys `optionalStep`).
 * - Eksisterende objekt overskrives; historikk håndteres ev. via object-versioning på bucketen.
 */
@Component
class RapportFilSkriver(
    private val gcpFilBucket: GcpFilBucket,
) {
    companion object {
        /** Tilsvarer bisys `DateUtil.formatCICSString` – brukes i filnavn og record-baserte filer. */
        val CICS_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        /** Tilsvarer bisys `DateUtil.formatOppdragString` – brukes i «Indeksdato»-linjen i FFU-filene. */
        val OPPDRAG_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }

    fun erKonfigurert(
        mappe: String?,
        filnavnRot: String?,
    ): Boolean = mappe != null && !filnavnRot.isNullOrBlank()

    /**
     * Bygger en [ByteArrayOutputStreamTilByteBuffer] med [innhold] og streamer den til
     * `<mappe><filnavnRot>-<indeksdato yyyyMMdd>.txt` i GCP-bucketen. Returnerer `true` hvis filen
     * ble lastet opp, `false` hvis mappe/filnavn mangler.
     */
    fun skrivFil(
        mappe: String?,
        filnavnRot: String?,
        indeksdato: LocalDate,
        innhold: String,
    ): Boolean {
        if (!erKonfigurert(mappe, filnavnRot)) {
            LOGGER.warn { "Hopper over rapportfil for '$filnavnRot' siden mappe eller filnavn ikke er konfigurert." }
            return false
        }

        val filnavn = "$mappe$filnavnRot-${indeksdato.format(CICS_FORMAT)}.txt"
        val buffer = ByteArrayOutputStreamTilByteBuffer().apply { write(innhold.toByteArray(Charsets.UTF_8)) }
        gcpFilBucket.lagreFil(filnavn, buffer, contentType = "text/plain")
        return true
    }
}
