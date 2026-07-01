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
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import org.hibernate.proxy.HibernateProxy
import java.sql.Timestamp

@Entity(name = "indeksregulering")
data class Indeksregulering(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Int? = null,
    @Column(name = "batch_id", nullable = false)
    var batchId: String,
    @Column(name = "ar", nullable = false)
    var år: Int,
    @ManyToOne
    @JoinColumn(name = "barn_id")
    var barn: Barn,
    @Column(name = "stonadstype", nullable = false)
    @Enumerated(EnumType.STRING)
    var stønadstype: Stønadstype,
    var begrunnelse: List<String> = emptyList(),
    @Enumerated(EnumType.STRING)
    var status: Status,
    var gjennomfort: Boolean = false,
    var vedtak: Int? = null,
    @Column(name = "opprettet_tidspunkt", nullable = false, updatable = false)
    var opprettetTidspunkt: Timestamp = Timestamp(System.currentTimeMillis()),
    var fattetTidspunkt: Timestamp? = null,
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
            "år = $år, " +
            "barn = $barn, " +
            "stønadstype = $stønadstype, " +
            "begrunnelse = $begrunnelse, " +
            "status = $status, " +
            "gjennomfort = $gjennomfort, " +
            "vedtak = $vedtak, " +
            "opprettetTidspunkt = $opprettetTidspunkt, " +
            "fattetTidspunkt = $fattetTidspunkt)"
}
