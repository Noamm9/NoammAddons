package noammaddons.features.hud

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.utils.ActionBarParser.SECRETS_REGEX
import noammaddons.utils.ActionBarParser.maxSecrets
import noammaddons.utils.ActionBarParser.secrets
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.RenderHelper.colorCodeByPresent
import noammaddons.utils.RenderHelper.getStringHeight
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.renderItem

object SecretDisplay: Feature() {
    private object SecretDisplayElement: GuiElement(hudData.getData().SecretDisplay) {
        override val enabled get() = config.secretDisplay
        var lines = listOf("&7Secrets", "&c3&7/&a7")
        val exampleLines = listOf("&7Secrets", "&c3&7/&a7")
        override val width: Float get() = lines.maxOf { getStringWidth(it) } + 16f
        override val height: Float get() = getStringHeight(lines)

        override fun draw() = draw(lines, getX(), getY(), getScale())
        override fun exampleDraw() = draw(exampleLines, getX(), getY(), getScale())
    }

    private val chestItem = ItemStack(Blocks.chest)

    @SubscribeEvent
    fun onRenderOvelay(event: RenderOverlay) {
        if (! SecretDisplayElement.enabled) return
        if (! inDungeon) return
        if (inBoss) return
        if (secrets == null) return
        if (maxSecrets == null) return

        SecretDisplayElement.lines = listOf("&7Secrets", "${colorCodeByPresent(secrets !!, maxSecrets !!)}$secrets&7/&a$maxSecrets")
        SecretDisplayElement.draw()
    }

    fun draw(text: List<String>, x: Float, y: Float, scale: Float) {
        val textWidth = text.maxOf { getStringWidth(it) + 16 } * scale
        val textHeight = text.size * 9f * scale
        val iconX = 38f

        GlStateManager.pushMatrix()
        GlStateManager.translate(x + iconX / 2, y, 1f)
        GlStateManager.translate(textWidth / 2f, textHeight / 2f, 0f)

        drawCenteredText(text, 0, 0, scale)
        renderItem(chestItem, - iconX * scale, - 9f * scale, scale)

        GlStateManager.popMatrix()
    }


    /**
     * @see noammaddons.mixins.MixinGuiIngame - modifyActionBar
     */
    @JvmStatic
    fun removeSecrets(s: String): String {
        if (! SecretDisplayElement.enabled) return s
        return s.replace(SECRETS_REGEX, "")
    }
}
