package no.nav.bidrag.automatiskjobb.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.math.BigDecimal
import java.sql.Timestamp

@Entity(name = "aldersjustering")
data class Aldersjustering(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    @Column(name = "batch_id", nullable = false)
    val batchId: String,
    @Column(name = "vedtaksid_beregning")
    var vedtaksidBeregning: Int? = null,
    @Column(name = "barn_id", nullable = false)
    val barnId: Int,
    @Column(name = "aldersgruppe", nullable = false)
    val aldersgruppe: Int,
    @Column(name = "lopende_belop")
    var lopendeBelop: BigDecimal? = null,
    @Column(name = "begrunnelse")
    var begrunnelse: List<String>? = null,
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: Status,
    @Column(name = "behandlingstype")
    @Enumerated(EnumType.STRING)
    var behandlingstype: Behandlingstype? = null,
    @Column(name = "vedtak")
    var vedtak: Int? = null,
    @Column(name = "oppgave")
    var oppgave: Int? = null,
    @Column(name = "opprettet_tidspunkt", nullable = false, updatable = false)
    val opprettetTidspunkt: Timestamp = Timestamp(System.currentTimeMillis()),
)

enum class Status {
    UBEHANDLET,
    TRUKKET,
    BEHANDLET,
    SLETTES,
    SLETTET,
    FEILET,
    FATTET,
}

enum class Behandlingstype {
    FATTET_FORSLAG,
    INGEN,
    FEILET,
    MANUELL,
}
