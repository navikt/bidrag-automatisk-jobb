package no.nav.bidrag.automatiskjobb.persistence.entity.enums

private const val DOKUMENTMAL_ALDERSJUSTERING_BI01B05 = "BI01B05"
private const val DOKUMENTMAL_REVURDERING_FORSKUDD_BI01A04 = "BI01A04"

enum class Forsendelsestype(
    val dokumentmal: String,
    val forsendelseTilBm: Boolean,
    val forsendelseTilBp: Boolean,
) {
    ALDERSJUSTERING_BIDRAG(DOKUMENTMAL_ALDERSJUSTERING_BI01B05, true, true),
    ALDERSJUSTERING_FORSKUDD("", true, false), // TODO(Legg litt alderjustering for forskudd her senere)
    REVURDERING_FORSKUDD(DOKUMENTMAL_REVURDERING_FORSKUDD_BI01A04, true, false),
}
