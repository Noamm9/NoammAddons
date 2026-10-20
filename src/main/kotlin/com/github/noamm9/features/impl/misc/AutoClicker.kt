package com.github.noamm9.features.impl.misc

//#if CHEAT

import com.github.noamm9.config.types.KeybindSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IKeyMapping
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.mojang.blaze3d.platform.InputConstants
import gg.essential.universal.UMinecraft
import net.minecraft.client.KeyMapping
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.*
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import org.lwjgl.sdl.SDLMouse
import kotlin.random.Random

object AutoClicker: Feature("Clicks for you while holding a keybind.") {
    private val leftClick by ToggleSetting("Left Click", true).section("Left Click")
    private val leftHold by ToggleSetting("Hold LMB").withDescription("Clicks while holding left click instead of a keybind.").showIf { leftClick.value }
    private val leftKey by KeybindSetting("Left Keybind").showIf { leftClick.value && ! leftHold.value }
    private val leftToggle by ToggleSetting("Left Toggle").withDescription("Press the keybind once to start and again to stop.").showIf { leftClick.value && ! leftHold.value }
    private val leftMinCps by SliderSetting("Left Min CPS", 9.0, 1.0, 20.0, 0.5).showIf { leftClick.value }
    private val leftMaxCps by SliderSetting("Left Max CPS", 12.0, 1.0, 20.0, 0.5).showIf { leftClick.value }
    private val breakBlocks by ToggleSetting("Break Blocks", true).withDescription("Stops clicking while looking at a block so you can mine it.").showIf { leftClick.value }

    private val rightClick by ToggleSetting("Right Click").section("Right Click")
    private val rightHold by ToggleSetting("Hold RMB").withDescription("Clicks while holding right click instead of a keybind.").showIf { rightClick.value }
    private val rightKey by KeybindSetting("Right Keybind").showIf { rightClick.value && ! rightHold.value }
    private val rightToggle by ToggleSetting("Right Toggle").withDescription("Press the keybind once to start and again to stop.").showIf { rightClick.value && ! rightHold.value }
    private val rightMinCps by SliderSetting("Right Min CPS", 7.0, 1.0, 20.0, 0.5).showIf { rightClick.value }
    private val rightMaxCps by SliderSetting("Right Max CPS", 10.0, 1.0, 20.0, 0.5).showIf { rightClick.value }
    private val ignoreInteractables by ToggleSetting("Ignore Interactables", true).withDescription("Doesn't click on chests, doors, levers and buttons.").showIf { rightClick.value }

    private val ignoreNpcs by ToggleSetting("Ignore NPCs", true).section("Entities")
    private val ignoreEntities by ToggleSetting("Ignore All Entities")

    private class Clicker {
        var nextClick = 0L
        var toggled = false
        var waitForRelease = false

        fun reset() {
            nextClick = 0L
            toggled = false
        }
    }

    private val left = Clicker()
    private val right = Clicker()

    override fun init() {
        register<TickEvent.Start> {
            val now = System.currentTimeMillis()
            tick(left, true, now)
            tick(right, false, now)
        }

        register<WorldChangeEvent> {
            left.reset()
            right.reset()
        }
    }

    override fun onDisable() {
        super.onDisable()
        left.reset()
        right.reset()
    }

    private fun tick(clicker: Clicker, isLeft: Boolean, now: Long) {
        val key = if (isLeft) mc.options.keyAttack else mc.options.keyUse
        val bind = if (isLeft) leftKey else rightKey
        val enabled = if (isLeft) leftClick.value else rightClick.value
        val hold = if (isLeft) leftHold.value else rightHold.value
        val toggleMode = ! hold && if (isLeft) leftToggle.value else rightToggle.value
        val usesVanillaKey = hold || isSameKey(bind, key)
        val down = if (hold) isPhysicallyDown(key) else bind.isDown()
        val pressed = bind.isPressed()

        if (! enabled) return clicker.reset()

        if (UMinecraft.currentScreenObj != null || ! mc.isWindowActive || ! mc.mouseHandler.isMouseGrabbed) {
            clicker.waitForRelease = true
            return clicker.reset()
        }

        if (clicker.waitForRelease) {
            if (down) return
            clicker.waitForRelease = false
        }

        if (toggleMode && pressed) clicker.toggled = ! clicker.toggled
        val active = if (toggleMode) clicker.toggled else down
        if (! active) {
            clicker.nextClick = 0L
            return
        }

        if (shouldPause(isLeft)) {
            if (usesVanillaKey && down) key.isDown = true
            clicker.nextClick = 0L
            return
        }

        if (usesVanillaKey && down) key.isDown = false

        if (clicker.nextClick == 0L) {
            if (usesVanillaKey) {
                clicker.nextClick = now + getDelay(isLeft)
                return
            }
            clicker.nextClick = now
        }

        var clicks = 0
        while (now >= clicker.nextClick && clicks < 2) {
            (key as IKeyMapping).clickCount += 1
            clicker.nextClick += getDelay(isLeft)
            clicks ++
        }

        if (now >= clicker.nextClick) clicker.nextClick = now + getDelay(isLeft)
    }

    private fun shouldPause(isLeft: Boolean): Boolean {
        val hit = mc.hitResult ?: return false
        if (hit.type == HitResult.Type.MISS) return false

        if (hit is EntityHitResult) return ignoreEntities.value || (ignoreNpcs.value && isNpc(hit.entity))
        if (hit !is BlockHitResult) return false

        val state = mc.level !!.getBlockState(hit.blockPos)
        return if (isLeft) breakBlocks.value && ! state.isAir
        else ignoreInteractables.value && isInteractable(state.block)
    }

    private fun getDelay(isLeft: Boolean): Long {
        val min = if (isLeft) leftMinCps.value else rightMinCps.value
        val max = if (isLeft) leftMaxCps.value else rightMaxCps.value
        val cps = if (min >= max) min else Random.nextDouble(min, max)
        return (1000 / cps).toLong()
    }

    private fun isSameKey(bind: KeybindSetting, key: KeyMapping): Boolean {
        val bound = (key as IKeyMapping).key
        return bind.value == bound.value && bind.isMouse == (bound.type == InputConstants.Type.MOUSE)
    }

    private fun isPhysicallyDown(key: KeyMapping): Boolean {
        val bound = (key as IKeyMapping).key
        return if (bound.type == InputConstants.Type.MOUSE) SDLMouse.SDL_GetMouseState(null, null) and (1 shl (bound.value - 1)) != 0
        else InputConstants.isKeyDown(bound.value)
    }

    private fun isNpc(entity: Entity): Boolean {
        if (entity.customName?.unformattedText == "CLICK") return true
        return entity.level().getEntities(entity, entity.boundingBox.move(0.0, - 1.0, 0.0)) { it is ArmorStand }
            .any { it.customName?.unformattedText == "CLICK" }
    }

    private fun isInteractable(block: Block) = when (block) {
        is BaseEntityBlock, is DoorBlock, is TrapDoorBlock, is FenceGateBlock, is LeverBlock, is ButtonBlock,
        is CraftingTableBlock, is AnvilBlock, is BedBlock, is NoteBlock, is DiodeBlock, is CakeBlock -> true
        else -> false
    }
}
//#endif
