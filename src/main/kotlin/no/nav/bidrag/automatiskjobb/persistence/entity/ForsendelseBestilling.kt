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
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.rolle.Rolletype
import org.hibernate.annotations.SQLRestriction
import java.sql.Timestamp

@Entity(name = "forsendelseBestilling")
@SQLRestriction(value = "slettet_tidspunkt is null")
data class ForsendelseBestilling(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override val id: Int? = null,
    @ManyToOne
    @JoinColumn(name = "aldersjustering_id")
    val aldersjustering: Aldersjustering,
    var forsendelseId: Long? = null,
    var journalpostId: Long? = null,
    @Enumerated(EnumType.STRING)
    val rolletype: Rolletype?,
    val mottaker: String? = null,
    @Column(name = "sprakkode")
    @Enumerated(EnumType.STRING)
    val språkkode: Språk? = null,
    val dokumentmal: String? = null,
    @Column(name = "opprettet_tidspunkt", nullable = false, updatable = false)
    val opprettetTidspunkt: Timestamp = Timestamp(System.currentTimeMillis()),
    var forsendelseOpprettetTidspunkt: Timestamp? = null,
    var distribuertTidspunkt: Timestamp? = null,
    var slettetTidspunkt: Timestamp? = null,
    val skalSlettes: Boolean = false,
) : EntityObject
