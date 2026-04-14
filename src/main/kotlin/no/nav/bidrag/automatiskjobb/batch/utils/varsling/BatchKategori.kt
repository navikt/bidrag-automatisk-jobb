package no.nav.bidrag.automatiskjobb.batch.utils.varsling

data class BatchKategori(
    val navn: String,
    val batcher: List<Batch>,
)

data class Batch(
    val navn: String,
    val cron: String,
)

