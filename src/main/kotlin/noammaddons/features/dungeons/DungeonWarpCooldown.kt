package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.events.Chat
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import kotlin.math.roundToInt

object DungeonWarpCooldown: Feature() {
    private object DungeonWarpCooldownElement: GuiElement(hudData.getData().dungeonWarpCooldown) {
        override val enabled get() = config.dungeonWarpCooldown
        override val height: Float get() = 9f
        override val width: Float
            get() {
                return if (mc.currentScreen is HudEditorScreen) getStringWidth(exampleText)
                else getStringWidth(text)
            }

        const val exampleText = "&bWarp Cooldown: &f30s"
        var text = ""

        override fun draw() = drawText(text, getX(), getY(), getScale())
        override fun exampleDraw() = drawText(exampleText, getX(), getY(), getScale())
    }

    private val floorEnterRegex = Regex("-+\\s.+ entered.+The Catacombs, Floor [IVX]+!\\s-+")
    private var onCd = false
    private var startTime = System.currentTimeMillis()


    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! DungeonWarpCooldownElement.enabled) return
        if (! event.component.noFormatText.matches(floorEnterRegex)) return
        if (onCd) return
        onCd = true
        startTime = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! DungeonWarpCooldownElement.enabled) return
        if (! onCd) return

        val remaining = (30 - (System.currentTimeMillis() - startTime) / 1000.0).roundToInt()
        if (remaining < 0) {
            onCd = false
            return
        }

        DungeonWarpCooldownElement.text = "&bWarp Cooldown: &f${remaining}s"
        DungeonWarpCooldownElement.draw()
    }
}
