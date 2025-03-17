package noammaddons.features.misc

import net.minecraft.client.gui.Gui
import net.minecraft.entity.boss.BossStatus
import noammaddons.features.Feature
import noammaddons.utils.MathUtils.interpolate
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawTexturedModalRect
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.awt.Color
import kotlin.math.pow

object SmoothBossBar: Feature() {
    private var smoothBossBarHealth = .0
    private fun resetBossBar() {
        smoothBossBarHealth = .0
    }

    @JvmStatic
    fun renderCustomBossBar(ci: CallbackInfo) {
        if (! config.smoothBossBarHealth) return resetBossBar()
        val name = BossStatus.bossName ?: return resetBossBar()
        if (BossStatus.statusBarTime -- <= 0) return resetBossBar()

        val targetHealth = BossStatus.healthScale
        val screenWidth = mc.getWidth()
        val barX = (screenWidth - 182) / 2
        val t = ((System.currentTimeMillis() % 1000) / 1000.0).coerceIn(0.0, 1.0)
        val easedT = if (t < 0.5) 4 * t * t * t else 1 - (- 2 * t + 2).pow(3) / 2

        smoothBossBarHealth = interpolate(smoothBossBarHealth, targetHealth, easedT)
        val barWidth = (smoothBossBarHealth * 184).toInt()

        mc.textureManager.bindTexture(Gui.icons)
        drawTexturedModalRect(barX, 12, 0, 74, 182, 5)
        if (barWidth > 0) drawTexturedModalRect(barX, 12, 0, 79, barWidth, 5)

        drawCenteredText(name, screenWidth / 2f, 2f, 1, Color.WHITE)

        ci.cancel()
    }
}
