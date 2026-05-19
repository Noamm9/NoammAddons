package com.github.noamm9.features.impl.misc

import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

object SnappyTappy: Feature("Prevents standing still when pressing opposing direction keys.") {
    private val pressTicks = mutableMapOf<KeyMapping, Long>()
    private val movementKeys by lazy {
        listOf(mc.options.keyLeft, mc.options.keyRight, mc.options.keyUp, mc.options.keyDown)
    }

    override fun init() {
        register<TickEvent.Start> {
            if (mc.screen != null) {
                if (pressTicks.isNotEmpty()) {
                    movementKeys.forEach { it.isDown = false }
                    pressTicks.clear()
                }
                return@register
            }

            for (key in movementKeys) {
                if (isKeyDown(key)) {
                    if (! pressTicks.containsKey(key)) {
                        pressTicks[key] = System.currentTimeMillis()
                    }
                    key.isDown = true
                }
                else {
                    pressTicks.remove(key)
                    key.isDown = false
                }
            }

            resolveConflict(mc.options.keyLeft, mc.options.keyRight)
            resolveConflict(mc.options.keyUp, mc.options.keyDown)
        }
    }

    private fun resolveConflict(a: KeyMapping, b: KeyMapping) {
        if (! a.isDown || ! b.isDown) return
        val timeA = pressTicks[a] ?: 0L
        val timeB = pressTicks[b] ?: 0L
        if (timeA >= timeB) b.isDown = false
        else a.isDown = false
    }

    private fun isKeyDown(key: KeyMapping): Boolean {
        val handle = mc.window.handle()
        val bound = KeyBindingHelper.getBoundKeyOf(key) ?: return false
        return if (bound.type == InputConstants.Type.MOUSE) {
            GLFW.glfwGetMouseButton(handle, bound.value) == GLFW.GLFW_PRESS
        }
        else GLFW.glfwGetKey(handle, bound.value) == GLFW.GLFW_PRESS
    }
}