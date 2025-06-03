package noammaddons.ui.config.core


import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.Component
import noammaddons.ui.config.core.impl.FeatureToggle


data class FeatureElement(
    val feature: Feature,
    val components: MutableSet<Component<out Any>>
) {
    val featureToggle = FeatureToggle(feature.name, this)
}