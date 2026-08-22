package com.github.noamm9.features.impl.misc

import com.github.noamm9.features.Feature
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMinecraft
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.world.entity.player.Input

object SnappyTappy: Feature("Prevents standing still when pressing opposing direction keys.") {
    private val pressTimes = mutableMapOf<KeyMapping, Long>()
    private val physicallyDown = mutableSetOf<KeyMapping>()

    @JvmStatic
    fun resolveInput(input: Input): Input {
        if (! enabled || UMinecraft.currentScreenObj != null) {
            physicallyDown.clear()
            pressTimes.clear()
            return input
        }

        val left = mc.options.keyLeft
        val right = mc.options.keyRight
        val forward = mc.options.keyUp
        val backward = mc.options.keyDown
        val keys = listOf(left, right, forward, backward)

        keys.forEach { key ->
            if (isPhysicallyDown(key)) {
                if (physicallyDown.add(key)) pressTimes[key] = System.nanoTime()
            }
            else {
                physicallyDown.remove(key)
                pressTimes.remove(key)
            }
        }

        var resolvedForward = input.forward()
        var resolvedBackward = input.backward()
        var resolvedLeft = input.left()
        var resolvedRight = input.right()

        if (forward in physicallyDown && backward in physicallyDown) {
            if (isNewer(forward, backward)) resolvedBackward = false else resolvedForward = false
        }
        if (left in physicallyDown && right in physicallyDown) {
            if (isNewer(left, right)) resolvedRight = false else resolvedLeft = false
        }

        return Input(
            resolvedForward,
            resolvedBackward,
            resolvedLeft,
            resolvedRight,
            input.jump(),
            input.shift(),
            input.sprint(),
        )
    }

    private fun isNewer(a: KeyMapping, b: KeyMapping) = (pressTimes[a] ?: 0L) >= (pressTimes[b] ?: 0L)
    private fun isPhysicallyDown(key: KeyMapping) = UKeyboard.isKeyDown(KeyMappingHelper.getBoundKeyOf(key).value)
}