package no.nav.bidrag.automatiskjobb.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Version
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
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
) : EntityObject {
    fun tilStønadsid(stønadstype: Stønadstype) =
        Stønadsid(
            stønadstype,
            Personident(kravhaver),
            Personident(skyldner!!),
            Saksnummer(saksnummer),
        )

    fun infoMedPerioder(): String =
        "Barn(id=$id, saksnummer='$saksnummer', fødselsdato=$fødselsdato, forskuddFra=$forskuddFra, forskuddTil=$forskuddTil, bidragFra=$bidragFra, bidragTil=$bidragTil)"

    fun infoUtenPerioder(): String =
        "Barn(id=$id, saksnummer='$saksnummer', kravhaver='$kravhaver', fødselsdato=$fødselsdato, skyldner=$skyldner)"

    override fun toString(): String =
        "Barn(id=$id, saksnummer='$saksnummer', kravhaver='$kravhaver', fødselsdato=$fødselsdato, skyldner=$skyldner, forskuddFra=$forskuddFra, forskuddTil=$forskuddTil, bidragFra=$bidragFra, bidragTil=$bidragTil, opprettet=$opprettet)"
}
