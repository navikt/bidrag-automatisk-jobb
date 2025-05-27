package no.nav.bidrag.automatiskjobb.utils

import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.transport.sak.BidragssakDto

val BidragssakDto.bidragsmottaker get() = roller.find { it.type == Rolletype.BIDRAGSMOTTAKER }
val BidragssakDto.bidragspliktig get() = roller.find { it.type == Rolletype.BIDRAGSPLIKTIG }

fun BidragssakDto.hentBarn(ident: String) =
    roller.find { it.type == Rolletype.BARN && it.fødselsnummer?.verdi == ident }
        ?: error("Fant ikke barn med ident $ident i sak $saksnummer")
