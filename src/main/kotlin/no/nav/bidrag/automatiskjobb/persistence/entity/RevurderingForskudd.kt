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
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import java.sql.Timestamp

@Entity(name = "revurdering_forskudd")
data class RevurderingForskudd(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override val id: Int? = null,
    @Column(name = "for_maned", nullable = false)
    val forMåned: String,
    @Column(name = "batch_id", nullable = false)
    val batchId: String,
    @ManyToOne
    @JoinColumn(name = "barn_id")
    val barn: Barn,
    var begrunnelse: List<String> = emptyList(),
    @Enumerated(EnumType.STRING)
    var status: Status,
    @Enumerated(EnumType.STRING)
    var behandlingstype: Behandlingstype? = null,
    var vedtaksidBeregning: Int? = null,
    var vedtak: Int? = null,
    var oppgave: Int? = null,
    @Column(name = "opprettet_tidspunkt", nullable = false, updatable = false)
    val opprettetTidspunkt: Timestamp = Timestamp(System.currentTimeMillis()),
    var fattetTidspunkt: Timestamp? = null,
    var resultatSisteVedtak: String? = null,
) : EntityObject {
    val unikReferanse get() = "revurdering_forskudd_${batchId}_${barn.tilStønadsid(Stønadstype.FORSKUDD).toReferanse()}"
    val begrunnelseVisningsnavn
        get() =
            begrunnelse.map { begrunnelse ->
                begrunnelse.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")
            }
}
