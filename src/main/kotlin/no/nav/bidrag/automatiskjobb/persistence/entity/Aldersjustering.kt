package no.nav.bidrag.automatiskjobb.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import java.math.BigDecimal
import java.sql.Timestamp

@Entity(name = "aldersjustering")
data class Aldersjustering(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override val id: Int? = null,
    @Column(name = "batch_id", nullable = false)
    val batchId: String,
    @Column(name = "vedtaksid_beregning")
    var vedtaksidBeregning: Int? = null,
    @ManyToOne
    @JoinColumn(name = "barn_id")
    val barn: Barn,
    @Column(name = "aldersgruppe", nullable = false)
    val aldersgruppe: Int,
    @Column(name = "lopende_belop")
    var lopendeBelop: BigDecimal? = null,
    @Column(name = "begrunnelse")
    var begrunnelse: List<String> = emptyList(),
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
    @Column(name = "fattet_tidspunkt")
    var fattetTidspunkt: Timestamp? = null,
    @Column(name = "stonadstype")
    @Enumerated(EnumType.STRING)
    val stønadstype: Stønadstype,
    @Column(name = "resultat_siste_vedtak")
    var resultatSisteVedtak: String? = null,
) : EntityObject

enum class Status {
    UBEHANDLET,
    TRUKKET,
    BEHANDLET,
    SIMULERT,
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
