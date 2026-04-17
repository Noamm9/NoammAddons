package com.github.noamm9.event.impl

import com.github.noamm9.event.Event
import net.minecraft.client.gui.screens.Screen

/**
 * Adapted from Firmament's ScreenChangeEvent.kt
 * Source: https://github.com/nea89o/Firmament/blob/master/src/main/kotlin/events/ScreenChangeEvent.kt
 */
class ScreenChangeEvent(val old: Screen?, val new: Screen?) : Event(cancelable = false) {
    var overrideScreen: Screen? = null
}
