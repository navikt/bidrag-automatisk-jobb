package no.nav.bidrag.automatiskjobb.persistence.entity

import no.nav.bidrag.domene.enums.vedtak.Stønadstype

interface ForsendelseEntity : EntityObject {
    val forsendelseBestilling: MutableList<ForsendelseBestilling>
    val unikReferanse: String
    val vedtak: Int?
    val stønadstype: Stønadstype
    val barn: Barn
    val batchId: String
}
