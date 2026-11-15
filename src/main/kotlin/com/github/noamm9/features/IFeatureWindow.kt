package com.github.noamm9.features

import com.github.noamm9.ui.clickgui.components.FeatureConfigWindow
import com.github.noamm9.ui.clickgui.components.FeatureWindow

interface IFeatureWindow {
    fun Feature.createWindow(): FeatureWindow = CBW(this)

    class CBW(feature: Feature): FeatureConfigWindow(feature) {
        override val useSidebar = true
    }
}