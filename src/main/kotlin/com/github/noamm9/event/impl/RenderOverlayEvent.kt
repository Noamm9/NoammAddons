package com.github.noamm9.event.impl

import com.github.noamm9.event.Event
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor

class RenderOverlayEvent(val context: GuiGraphicsExtractor, val deltaTracker: DeltaTracker): Event(cancelable = false)