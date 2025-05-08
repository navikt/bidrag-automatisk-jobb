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
import java.sql.Timestamp

@Entity(name = "forsendelseBestilling")
data class ForsendelseBestilling(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    @ManyToOne
    @JoinColumn(name = "aldersjustering_id")
    val aldersjustering: Aldersjustering,
    @Column(name = "forsendelse_id")
    var forsendelseId: Long? = null,
    @Column(name = "journalpost_id")
    var journalpostId: Long? = null,
    @Column(name = "rolletype")
    @Enumerated(EnumType.STRING)
    val rolletype: Rolletype?,
    @Column(name = "mottaker")
    val mottaker: String? = null,
    @Column(name = "sprakkode")
    @Enumerated(EnumType.STRING)
    val språkkode: Språk? = null,
    @Column(name = "dokumentmal")
    val dokumentmal: String? = null,
    @Column(name = "opprettet_tidspunkt", nullable = false, updatable = false)
    val opprettetTidspunkt: Timestamp = Timestamp(System.currentTimeMillis()),
    @Column(name = "bestilt_tidspunkt")
    var bestiltTidspunkt: Timestamp? = null,
    @Column(name = "distribuer_tidspunkt")
    var distribuerTidspunkt: Timestamp? = null,
    @Column(name = "slettet_tidspunkt")
    var slettetTidspunkt: Timestamp? = null,
)
