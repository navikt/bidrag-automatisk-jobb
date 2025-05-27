package no.nav.bidrag.automatiskjobb.utils

import io.getunleash.variant.Variant
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider

enum class UnleashFeatures(
    val featureName: String,
    defaultValue: Boolean,
) {
    OPPRETT_REVURDER_FORSKUDD_OPPGAVE("automatiskjobb.opprett-revurder-forskudd-oppgave", false),
    ;

    private var defaultValue = false

    init {
        this.defaultValue = defaultValue
    }

    val isEnabled: Boolean
        get() = UnleashFeaturesProvider.isEnabled(featureName, defaultValue)

    val variant: Variant?
        get() = UnleashFeaturesProvider.getVariant(featureName)
}
