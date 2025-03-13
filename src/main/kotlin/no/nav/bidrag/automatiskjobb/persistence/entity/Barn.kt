package no.nav.bidrag.automatiskjobb.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Version
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(name = "barn")
data class Barn(
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0,
    @Column(name = "saksnummer")
    var saksnummer: String = "",
    @Column(name = "kravhaver")
    var kravhaver: String = "",
    @Column(name = "fodselsdato")
    var fødselsdato: LocalDate = LocalDate.now(),
    @Column(name = "skyldner")
    var skyldner: String? = null,
    @Column(name = "forskudd_fra")
    var forskuddFra: LocalDate? = null,
    @Column(name = "forskudd_til")
    var forskuddTil: LocalDate? = null,
    @Column(name = "bidrag_fra")
    var bidragFra: LocalDate? = null,
    @Column(name = "bidrag_til")
    var bidragTil: LocalDate? = null,
    @Version
    @Column(name = "opprettet")
    var opprettet: LocalDateTime? = null,
)
