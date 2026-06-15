package no.nav.bidrag.automatiskjobb.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.OrderBy
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.hibernate.proxy.HibernateProxy
import java.sql.Timestamp

/**
 * Felles indeksregulering-post som kobler en sak (saksnummer) til alle barn som inngår i
 * indeksreguleringen for et gitt år. Entiteten støtter både forskudd ([Stønadstype.FORSKUDD])
 * og bidrag ([Stønadstype.BIDRAG]).
 *
 * [gjennomfort] settes til `true` av gjennomføringsbatchen etter at indeksreguleringen er utført.
 * Opprettingsbatchen hopper over saker der [gjennomfort] allerede er `true` for inneværende år.
 *
 * Alle barn på samme sak ligger under samme rad (én rad per sak og år), på samme måte som
 * [RevurderingForskudd].
 */
@Entity(name = "indeksregulering")
data class Indeksregulering(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override val id: Int? = null,
    @Column(name = "batch_id", nullable = false)
    val batchId: String,
    @Column(nullable = false)
    val saksnummer: String,
    @Column(name = "ar", nullable = false)
    val år: Int,
    @field:ManyToMany(fetch = FetchType.EAGER)
    @field:JoinTable(
        name = "indeksregulering_barn",
        joinColumns = [JoinColumn(name = "indeksregulering_id")],
        inverseJoinColumns = [JoinColumn(name = "barn_id")],
    )
    @field:OrderBy("id")
    val barn: MutableList<Barn>,
    @Column(name = "stonadstype", nullable = false)
    @Enumerated(EnumType.STRING)
    val stønadstype: Stønadstype = Stønadstype.FORSKUDD,
    var begrunnelse: List<String> = emptyList(),
    @Enumerated(EnumType.STRING)
    var status: Status,
    var gjennomfort: Boolean = false,
    var vedtak: Int? = null,
    @Column(name = "opprettet_tidspunkt", nullable = false, updatable = false)
    val opprettetTidspunkt: Timestamp = Timestamp(System.currentTimeMillis()),
    var fattetTidspunkt: Timestamp? = null,
    var resultatSisteVedtak: String? = null,
) : EntityObject {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisEffectiveClass = this.javaClass
        if (thisEffectiveClass != oEffectiveClass) return false
        other as Indeksregulering

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String =
        this::class.simpleName + "(" +
            "id = $id, " +
            "batchId = $batchId, " +
            "saksnummer = $saksnummer, " +
            "år = $år, " +
            "barn = $barn, " +
            "stønadstype = $stønadstype, " +
            "begrunnelse = $begrunnelse, " +
            "status = $status, " +
            "gjennomfort = $gjennomfort, " +
            "vedtak = $vedtak, " +
            "opprettetTidspunkt = $opprettetTidspunkt, " +
            "fattetTidspunkt = $fattetTidspunkt, " +
            "resultatSisteVedtak = $resultatSisteVedtak)"
}
