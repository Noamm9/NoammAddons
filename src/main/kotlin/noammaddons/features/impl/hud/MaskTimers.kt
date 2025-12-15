package noammaddons.features.impl.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.editgui.GuiElement
import noammaddons.config.editgui.HudEditorScreen
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.events.ServerTick
import noammaddons.events.WorldUnloadEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.SeperatorSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.LocationUtils
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper.colorCodeByPresent
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getStringHeight
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.Utils.remove

object MaskTimers: Feature() {
    object MaskTimersElement: GuiElement(hudData.getData().maskTimers) {
        var text = ""
        private val exampleText = Masks.entries.joinToString("\n") { "${it.color}${it.maskName}: &aREADY" }

        override val enabled get() = MaskTimers.enabled
        override val width: Float get() = if (mc.currentScreen is HudEditorScreen) getStringWidth(exampleText) else getStringWidth(text)
        override val height: Float get() = if (mc.currentScreen is HudEditorScreen) getStringHeight(exampleText) else getStringHeight(text)

        override fun draw() = drawText(text, getX(), getY(), getScale())
        override fun exampleDraw() = drawText(exampleText, getX(), getY(), getScale())
    }

    private val bonzo = ToggleSetting("Bonzo")
    private val phoenix = ToggleSetting("Phoenix")
    private val spirit = ToggleSetting("Spirit")
    private val popAlert = ToggleSetting("Pop Alert")
    private val invulnerabilityTimers = ToggleSetting("Invulnerability Timers")


    init {
        addSettings(
            bonzo, phoenix, spirit,
            SeperatorSetting("Extra"),
            popAlert, invulnerabilityTimers
        )

        MaskTimersElement
    }

    enum class Masks(
        val maskName: String,
        val color: String,
        val regex: Regex,
        val cooldown: Int,
        val displayConfig: () -> Boolean,
        val invulnerabilityTime: Int
    ) {
        PHOENIX_PET(
            "Phoenix Pet",
            "&c",
            Regex("^Your Phoenix Pet saved you from certain death!$"),
            60 * 20,
            { phoenix.value },
            20 * 4
        ),
        SPIRIT_MASK(
            "Spirit Mask",
            "&f",
            Regex("^Second Wind Activated! Your Spirit Mask saved your life!$"),
            30 * 20,
            { spirit.value },
            3 * 20
        ),
        BONZO_MASK(
            "Bonzo Mask",
            "&9",
            Regex("^Your (?:. )?Bonzo's Mask saved your life!$"),
            180 * 20,
            { bonzo.value },
            3 * 20
        );

        var draw = false
        var timer = - 40
        var invTicks = - 1
        val cooldownTime get() = timer / 20f

        companion object {
            val activeMasks = mutableSetOf<Masks>()

            fun updateTimers() {
                val iterator = activeMasks.iterator()
                while (iterator.hasNext()) {
                    val mask = iterator.next()
                    if (mask.invTicks != - 1) mask.invTicks --
                    if (mask.timer > - 40) mask.timer --
                    else {
                        mask.draw = false
                        iterator.remove()
                    }
                }
            }

            fun reset() {
                activeMasks.clear()
                Masks.entries.forEach {
                    it.draw = false
                    it.timer = - 40
                    it.invTicks = - 1
                }
            }
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) = Masks.updateTimers()

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) = Masks.reset()

    @SubscribeEvent
    fun onChat(event: Chat) = with(event.component.noFormatText) {
        if (! LocationUtils.inSkyblock) return@onChat
        for (mask in Masks.entries) {
            if (! mask.regex.matches(this)) continue
            mask.timer = mask.cooldown
            mask.draw = mask.displayConfig()
            if (mask.draw) Masks.activeMasks.add(mask)
            if (popAlert.value) showTitle("${mask.color}${mask.maskName}")
            if (invulnerabilityTimers.value) mask.invTicks = mask.invulnerabilityTime
        }
    }

    @SubscribeEvent
    fun onRender(event: RenderOverlay) {
        if (! LocationUtils.inSkyblock) return
        val masks = Masks.activeMasks.toList().takeUnless { it.isEmpty() } ?: return

        MaskTimersElement.text = masks.joinToString("\n") { mask ->
            if (mask.cooldownTime > 0) "${mask.color}${mask.maskName}: &a${mask.cooldownTime.toFixed(1)}"
            else "${mask.color}${mask.maskName}: &aREADY"
        }
        MaskTimersElement.draw()


        masks.maxByOrNull { it.invTicks }?.takeIf { it.invTicks != - 1 }?.let { mask ->
            val str = mask.color + mask.maskName.remove("Pet", "Mask", " ") + ": ${colorCodeByPresent(mask.invTicks, mask.invulnerabilityTime)}${mask.invTicks}"
            val x = mc.getWidth() / 2f
            val y = mc.getHeight() / 3f
            val s = 1.5f

            drawCenteredText(str, x, y, s)
        }
    }
}
