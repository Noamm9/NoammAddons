package com.github.noamm9.event.impl

import com.github.noamm9.event.Event
import com.github.noamm9.utils.render.world.RenderContext

class RenderWorldEvent(val ctx: RenderContext): Event(cancelable = false)