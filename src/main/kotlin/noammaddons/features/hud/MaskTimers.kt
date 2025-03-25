package noammaddons.features.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.LocationUtils
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper.getStringHeight
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText

object MaskTimers: Feature() {
    object MaskTimersElement: GuiElement(hudData.getData().MaskTimers) {
        var text = ""
        private val exampleText = Masks.entries.joinToString("\n") { "${it.color}${it.maskName}: &aREADY" }

        override val enabled get() = true
        override val width: Float get() = if (mc.currentScreen is HudEditorScreen) getStringWidth(exampleText) else getStringWidth(text)
        override val height: Float get() = if (mc.currentScreen is HudEditorScreen) getStringHeight(exampleText) else getStringHeight(text)

        override fun draw() = drawText(text, getX(), getY(), getScale())
        override fun exampleDraw() = drawText(exampleText, getX(), getY(), getScale())
    }

    // Auto-registering the element
    init {
        MaskTimersElement
    }

    enum class Masks(
        val maskName: String,
        val color: String,
        val regex: Regex,
        val cooldown: Int,
        val displayConfig: () -> Boolean,
        val alertConfig: () -> Boolean
    ) {
        PHOENIX_PET(
            "Phoenix Pet",
            "&c",
            Regex("^Your Phoenix Pet saved you from certain death!$"),
            60 * 20,
            { config.PhoenixPetDisplay },
            { config.PhoenixPetAlert }
        ),
        SPIRIT_MASK(
            "Spirit Mask",
            "&f",
            Regex("^Second Wind Activated! Your Spirit Mask saved your life!$"),
            30 * 20,
            { config.SpiritMaskDisplay },
            { config.SpiritMaskAlert }
        ),
        BONZO_MASK(
            "Bonzo Mask",
            "&9",
            Regex("^Your (?:. )?Bonzo's Mask saved your life!$"),
            180 * 20,
            { config.BonzoMaskDisplay },
            { config.BonzoMaskAlert }
        );

        var draw = false
        var timer = - 40
        val cooldownTime get() = timer / 20f

        companion object {
            val activeMasks = mutableSetOf<Masks>()

            fun updateTimers() {
                val iterator = activeMasks.iterator()
                while (iterator.hasNext()) {
                    val mask = iterator.next()
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
                }
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! LocationUtils.inSkyblock) return

        for (mask in Masks.entries) {
            if (! mask.regex.matches(event.component.noFormatText)) continue
            mask.timer = mask.cooldown
            mask.draw = mask.displayConfig()
            if (mask.draw) Masks.activeMasks.add(mask)
            if (mask.alertConfig()) showTitle("${mask.color}${mask.maskName}")
        }
    }

    @SubscribeEvent
    fun onRender(event: RenderOverlay) {
        if (! LocationUtils.inSkyblock) return
        if (Masks.activeMasks.isEmpty()) return

        MaskTimersElement.text = Masks.activeMasks.joinToString("\n") { mask ->
            if (mask.cooldownTime > 0) "${mask.color}${mask.maskName}: &a${mask.cooldownTime.toFixed(1)}"
            else "${mask.color}${mask.maskName}: &aREADY"
        }

        MaskTimersElement.draw()
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) = Masks.updateTimers()

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) = Masks.reset()
}
