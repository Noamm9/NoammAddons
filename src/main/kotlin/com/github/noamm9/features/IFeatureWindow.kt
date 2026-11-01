package com.github.noamm9.features

import com.github.noamm9.ui.clickgui.components.FeatureConfigWindow

interface IFeatureWindow {
    fun createWindow(): FeatureConfigWindow

    class CBW(feature: Feature): FeatureConfigWindow(feature) {
        override val useSidebar = true
    }
}