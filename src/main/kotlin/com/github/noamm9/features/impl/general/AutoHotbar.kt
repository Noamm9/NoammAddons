package com.github.noamm9.features.impl.general

//#if CHEAT

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.MOD_NAME
import com.github.noamm9.event.EventPriority
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IKeyMapping
import com.github.noamm9.ui.clickgui.ClickGuiScreen
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.ui.gui.AutoHotbarScreen
import com.github.noamm9.utils.JsonUtils
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.ServerUtils
import com.github.noamm9.utils.items.ItemUtils.itemUUID
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.ItemStack
import java.io.File
import java.io.FileReader
import java.util.*

object AutoHotbar: Feature("Automatically swaps items to specific hotbar slots upon chat triggers.") {
    private val configFile = File(mc.gameDirectory, "config/$MOD_NAME/autoHotbar.json")
    var config = AutoSwapConfig(mutableMapOf(), mutableMapOf(), mutableSetOf())

    val disabledTriggers: MutableSet<String>
        get() {
            val current = config.disabled
            if (current != null) return current
            val created = mutableSetOf<String>()
            config.disabled = created
            return created
        }

    fun isTriggerEnabled(trigger: String) = trigger !in disabledTriggers

    fun setTriggerEnabled(trigger: String, enabled: Boolean) {
        if (enabled) disabledTriggers.remove(trigger) else disabledTriggers.add(trigger)
    }

    private val swapDelay by SliderSetting("Base Delay", 100, 0, 500, 5).withDescription("How much time to wait between slot swapping").section("Timing (ms)")
    private val jitter by SliderSetting("Random Delay", 50, 0, 100, 1).withDescription("Random Delay to add on top of the Base Delay")
    private val pingDelay by SliderSetting("Stop Delay", 200, 0, 500, 5).withDescription("how much time to wait after you are not moving.")
    private val showTitles by ToggleSetting("HUD Info", true).withDescription("Shows the currnt auto swap progress on screen")
    private val openGuiBtn by ButtonSetting("Open Config") {
        ClickGuiScreen.selectedFeature = null
        NoammAddons.screen = AutoHotbarScreen()
    }.withDescription("Opens the GUI to visually map your inventory to hotbar slots.")

    private val swapQueue = ArrayDeque<SwapRule>()
    private val triggeredMessages = mutableSetOf<String>()
    private var selectedItem: ItemStack? = null

    private var nextSwapTime = 0L
    private var isSwapping = false
    private var stationaryTicks = 0
    private var movementLockEndTime = 0L

    private val movementKeys by lazy {
        listOf(
            mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight,
            mc.options.keyJump, mc.options.keySprint, mc.options.keyShift
        )
    }

    override fun init() {
        loadConfig()
        Runtime.getRuntime().addShutdownHook(Thread { saveConfig() })

        register<WorldChangeEvent> {
            swapQueue.clear()
            triggeredMessages.clear()
            selectedItem = null
            isSwapping = false
            stationaryTicks = 0
            movementLockEndTime = 0L
        }

        register<ChatMessageEvent>(EventPriority.HIGH) {
            val key = config.triggers.entries.find { it.value == event.unformattedText }?.key ?: return@register
            if (key in disabledTriggers) return@register
            if (key in triggeredMessages) return@register
            val rules = config.rules[key] ?: return@register
            if (rules.isEmpty()) return@register
            triggeredMessages.add(key)

            val inv = mc.player?.inventory?.withIndex()
            val swaps = rules.filter { rule ->
                inv?.find { (i, stack) -> stack.itemUUID.ifBlank { stack.skyblockId }.ifBlank { stack.hoverName.string } == rule.id && i != rule.hotbarSlot + 36 } != null
            }

            swapQueue.clear()
            swapQueue.addAll(swaps)
            nextSwapTime = System.currentTimeMillis() + pingDelay.value.toLong()
        }

        register<TickEvent.Start> {
            if (! isSwapping && System.currentTimeMillis() >= movementLockEndTime) return@register
            movementKeys.forEach { it.isDown = false }
            (mc.options.keyUse as IKeyMapping).clickCount = 0
            (mc.options.keyAttack as IKeyMapping).clickCount = 0
            mc.player?.isSprinting = false

        }

        register<TickEvent.Start> {
            if (swapQueue.isEmpty()) {
                if (isSwapping) {
                    isSwapping = false
                    movementLockEndTime = System.currentTimeMillis() + 250
                }
                return@register
            }

            val player = mc.player ?: return@register
            val now = System.currentTimeMillis()

            if (player.deltaMovement.horizontalDistanceSqr() > 0.001) {
                stationaryTicks = 0
                nextSwapTime = now + 250
                return@register
            }

            stationaryTicks ++

            if (stationaryTicks < 10) return@register
            if (now < nextSwapTime) return@register
            if (ServerUtils.tps < 18f || ServerUtils.currentPing > 500) return@register
            if (mc.screen != null) {
                stationaryTicks = 0
                nextSwapTime = now + pingDelay.value.toLong()
                return@register
            }

            isSwapping = true
            val rule = swapQueue.removeFirst()
            val container = player.containerMenu

            val foundSlot = container.slots.find {
                it.item.itemUUID.ifBlank { it.item.skyblockId }.ifBlank { it.item.hoverName.string } == rule.id
            } ?: return@register

            if (foundSlot.index == rule.hotbarSlot + 36) {
                nextSwapTime = System.currentTimeMillis() + MathUtils.gaussianRandom(swapDelay.value, swapDelay.value + jitter.value)
                return@register
            }

            mc.gameMode?.handleInventoryMouseClick(container.containerId, foundSlot.index, rule.hotbarSlot, ClickType.SWAP, player)
            nextSwapTime = System.currentTimeMillis() + MathUtils.gaussianRandom(swapDelay.value, swapDelay.value + jitter.value)
        }

        hudElement("AutoSwap Status", enabled = { showTitles.value }, shouldDraw = { swapQueue.isNotEmpty() }, centered = true) { ctx, example ->
            val text = if (example) "&bSwapping &f3 left"
            else if (stationaryTicks < 10) "&cStop Moving!"
            else "&bSwapping &f${swapQueue.size} left"

            Render2D.drawCenteredString(ctx, text, 0, 0)
            return@hudElement text.width().toFloat() to 9f
        }
    }

    private fun loadConfig() {
        if (! configFile.exists()) return
        config = FileReader(configFile).use {
            JsonUtils.gsonBuilder.fromJson(it, AutoSwapConfig::class.java)
        }
    }

    fun saveConfig() {
        configFile.parentFile.mkdirs()
        JsonUtils.toJson(configFile, config)
    }

    data class SwapRule(val id: String, val hotbarSlot: Int)

    data class AutoSwapConfig(
        var triggers: MutableMap<String, String>,
        var rules: MutableMap<String, MutableList<SwapRule>>,
        var disabled: MutableSet<String>
    )
}
//#endif