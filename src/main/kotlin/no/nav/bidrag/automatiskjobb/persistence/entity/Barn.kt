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
    override var id: Int? = null,
    var saksnummer: String = "",
    var kravhaver: String = "",
    @Column(name = "fodselsdato")
    var fødselsdato: LocalDate? = null,
    var skyldner: String? = null,
    var forskuddFra: LocalDate? = null,
    var forskuddTil: LocalDate? = null,
    var bidragFra: LocalDate? = null,
    var bidragTil: LocalDate? = null,
    @Version
    var opprettet: LocalDateTime? = null,
) : EntityObject
