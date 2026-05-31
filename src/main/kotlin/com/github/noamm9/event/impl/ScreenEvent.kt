package com.github.noamm9.event.impl

import com.github.noamm9.event.Event
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen

abstract class ScreenEvent(val screen: Screen): Event(cancelable = true) {
    class PreRender(screen: Screen, val context: GuiGraphicsExtractor, val mouseX: Int, val mouseY: Int): ScreenEvent(screen)
    class PostRender(screen: Screen, val context: GuiGraphicsExtractor, val mouseX: Int, val mouseY: Int): ScreenEvent(screen)
}