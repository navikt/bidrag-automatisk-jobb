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
    var vedtaksidBeregning: Int? = null,
    @ManyToOne
    @JoinColumn(name = "barn_id")
    val barn: Barn,
    val aldersgruppe: Int,
    var lopendeBelop: BigDecimal? = null,
    var begrunnelse: List<String> = emptyList(),
    @Enumerated(EnumType.STRING)
    var status: Status,
    @Enumerated(EnumType.STRING)
    var behandlingstype: Behandlingstype? = null,
    var vedtak: Int? = null,
    var oppgave: Int? = null,
    @Column(name = "opprettet_tidspunkt", nullable = false, updatable = false)
    val opprettetTidspunkt: Timestamp = Timestamp(System.currentTimeMillis()),
    var fattetTidspunkt: Timestamp? = null,
    @Column(name = "stonadstype")
    @Enumerated(EnumType.STRING)
    val stønadstype: Stønadstype = Stønadstype.BIDRAG,
    var resultatSisteVedtak: String? = null,
) : EntityObject {
    val aldersjusteresForÅr get() = barn.fødselsdato!!.year + aldersgruppe
    val unikReferanse get() = "aldersjustering_${batchId}_${barn.tilStønadsid(stønadstype).toReferanse()}"
    val begrunnelseVisningsnavn get() =
        begrunnelse.map { it.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ") }
}

enum class Status {
    UBEHANDLET,
    TRUKKET,
    BEHANDLET,
    SIMULERT,
    SLETTES,
    SLETTET,
    FEILET,
    FATTE_VEDTAK_FEILET,
    FATTET,
}

enum class Behandlingstype {
    FATTET_FORSLAG,
    INGEN,
    FEILET,
    MANUELL,
}
