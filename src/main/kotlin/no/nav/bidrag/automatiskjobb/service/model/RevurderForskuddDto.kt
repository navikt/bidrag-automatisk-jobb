package no.nav.bidrag.automatiskjobb.service.model

import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer

data class AdresseEndretResultat(
    val saksnummer: String,
    val enhet: String,
    val bidragsmottaker: String,
    val gjelderBarn: String,
)

data class ForskuddRedusertResultat(
    val saksnummer: String,
    val bidragsmottaker: String,
    val gjelderBarn: String,
    val stønadstype: Stønadstype? = null,
    val engangsbeløptype: Engangsbeløptype? = null,
)

data class StønadEngangsbeløpId(
    val kravhaver: Personident,
    val skyldner: Personident,
    val sak: Saksnummer,
    val engangsbeløptype: Engangsbeløptype? = null,
    val stønadstype: Stønadstype? = null,
)
