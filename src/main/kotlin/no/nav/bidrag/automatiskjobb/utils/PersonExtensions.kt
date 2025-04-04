package no.nav.bidrag.automatiskjobb.utils

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.HusstandsmedlemmerDto

fun HusstandsmedlemmerDto.erHusstandsmedlem(personident: Personident) =
    husstandListe.any { hl ->
        hl.husstandsmedlemListe.any { it.personId == personident }
    }
