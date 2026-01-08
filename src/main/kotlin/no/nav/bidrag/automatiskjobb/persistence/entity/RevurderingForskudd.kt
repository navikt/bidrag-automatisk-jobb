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
import jakarta.persistence.OneToMany
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.felles.personidentNav
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
import org.hibernate.proxy.HibernateProxy
import java.sql.Timestamp

@Entity(name = "revurdering_forskudd")
data class RevurderingForskudd(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override val id: Int? = null,
    @Column(name = "for_maned", nullable = false)
    val forMåned: String,
    @Column(name = "batch_id", nullable = false)
    override val batchId: String,
    @ManyToOne
    @JoinColumn(name = "barn_id")
    override val barn: Barn,
    var begrunnelse: List<String> = emptyList(),
    @Enumerated(EnumType.STRING)
    var status: Status,
    @Enumerated(EnumType.STRING)
    var behandlingstype: Behandlingstype? = null,
    var vurdereTilbakekreving: Boolean = false,
    var vedtaksidBeregning: Int? = null,
    override var vedtak: Int? = null,
    var oppgave: Int? = null,
    @Column(name = "opprettet_tidspunkt", nullable = false, updatable = false)
    val opprettetTidspunkt: Timestamp = Timestamp(System.currentTimeMillis()),
    var fattetTidspunkt: Timestamp? = null,
    var resultatSisteVedtak: String? = null,
    @Column(name = "stonadstype")
    @Enumerated(EnumType.STRING)
    override val stønadstype: Stønadstype = Stønadstype.FORSKUDD,
    @OneToMany
    @JoinColumn(name = "revurdering_forskudd_id")
    override val forsendelseBestilling: MutableList<ForsendelseBestilling> = mutableListOf(),
) : ForsendelseEntity {
    override val unikReferanse get() = "revurdering_forskudd_${batchId}_${tilStønadsidReferanse()}"

    fun tilStønadsidReferanse(): String {
        return Stønadsid(
            Stønadstype.FORSKUDD,
            Personident(barn.kravhaver),
            personidentNav,
            Saksnummer(barn.saksnummer),
        ).toReferanse()
    }
    val begrunnelseVisningsnavn
        get() =
            begrunnelse.map { begrunnelse ->
                begrunnelse.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")
            }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisEffectiveClass =
            this.javaClass
        if (thisEffectiveClass != oEffectiveClass) return false
        other as RevurderingForskudd

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String =
        this::class.simpleName + "(" +
            "id = $id, " +
            "forMåned = $forMåned, " +
            "batchId = $batchId, " +
            "barn = $barn, " +
            "begrunnelse = $begrunnelse, " +
            "status = $status, " +
            "behandlingstype = $behandlingstype, " +
            "vurdereTilbakekreving = $vurdereTilbakekreving, " +
            "vedtaksidBeregning = $vedtaksidBeregning, " +
            "vedtak = $vedtak, " +
            "oppgave = $oppgave, " +
            "opprettetTidspunkt = $opprettetTidspunkt, " +
            "fattetTidspunkt = $fattetTidspunkt, " +
            "resultatSisteVedtak = $resultatSisteVedtak, " +
            "stønadstype = $stønadstype)"
}
