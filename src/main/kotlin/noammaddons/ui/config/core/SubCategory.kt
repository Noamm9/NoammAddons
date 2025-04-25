package noammaddons.ui.config.core


import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.Component
import noammaddons.ui.config.core.impl.button1


data class SubCategory(
    val feature: Feature,
    val components: MutableSet<Component<out Any>>
) {
    val button1 = button1(feature.name, this)
}