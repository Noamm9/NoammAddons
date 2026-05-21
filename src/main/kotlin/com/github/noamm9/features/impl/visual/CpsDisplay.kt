package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width

object CpsDisplay: Feature("Displays your left and right clicks per second.") {
    private val leftClicks = mutableListOf<Long>()
    private val rightClicks = mutableListOf<Long>()

    override fun init() {
        hudElement("CPS Display") { ctx, _ ->
            val l = getCps(leftClicks)
            val r = getCps(rightClicks)
            val text = "§f$l §7| §f$r §bCPS"
            Render2D.drawString(ctx, text, 2f, 2f)
            return@hudElement text.width().toFloat() + 4f to 12f
        }

        register<WorldChangeEvent> {
            leftClicks.clear()
            rightClicks.clear()
        }
    }

    private fun getCps(list: MutableList<Long>): Int {
        val now = System.currentTimeMillis()
        list.removeIf { now - it > 1000 }
        return list.size
    }

    @JvmStatic
    fun addLeftClick() {
        if (! enabled) return
        leftClicks.add(System.currentTimeMillis())
    }

    @JvmStatic
    fun addRightClick() {
        if (! enabled) return
        rightClicks.add(System.currentTimeMillis())
    }
}