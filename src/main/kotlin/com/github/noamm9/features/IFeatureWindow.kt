package com.github.noamm9.features

import com.github.noamm9.ui.clickgui.components.FeatureConfigWindow

interface IFeatureWindow {
    fun Feature.createWindow(): FeatureConfigWindow = CBW(this)

    class CBW(feature: Feature): FeatureConfigWindow(feature) {
        override val useSidebar = true
    }
}