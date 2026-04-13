package com.github.noamm9.event.impl

import com.github.noamm9.event.Event
import net.minecraft.client.gui.screens.Screen

class ScreenChangeEvent(val old: Screen?, val new: Screen?) : Event(cancelable = false) {
    var overrideScreen: Screen? = null
}
