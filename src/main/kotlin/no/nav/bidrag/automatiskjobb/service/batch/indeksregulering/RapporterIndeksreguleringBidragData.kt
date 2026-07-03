package no.nav.bidrag.automatiskjobb.service.batch.indeksregulering

data class RapporterIndeksreguleringBidragData(
    val bidragsreskontro: List<RapportLinje> = emptyList(),
    val bpUtlandBrev: List<RapportLinje> = emptyList(),
    val bpUtlandDiskresjon: List<RapportLinje> = emptyList(),
    val bpUtlandManglerAdresse: List<RapportLinje> = emptyList(),
    val elin: List<RapportLinje> = emptyList(),
)
