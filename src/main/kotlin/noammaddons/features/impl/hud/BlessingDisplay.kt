package noammaddons.features.impl.hud

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.DungeonUtils.Blessing
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.RenderHelper
import noammaddons.utils.RenderUtils
import java.awt.Color

object BlessingDisplay: Feature("Displays the current active blessings of the dungeon.") {
    private val power by ToggleSetting("Power Blessing", true)
    private val time by ToggleSetting("Time Blessing", true)
    private val wisdom by ToggleSetting("Wisdom Blessing", false)
    private val life by ToggleSetting("Life Blessing", false)
    private val stone by ToggleSetting("Stone Blessing", false)
    private val s by SeperatorSetting("Colors")
    private val powerColor by ColorSetting("Power Color", Color(mc.fontRendererObj.getColorCode('4'))).hideIf { ! power }
    private val timeColor by ColorSetting("Time Color", Color(mc.fontRendererObj.getColorCode('5'))).hideIf { ! time }
    private val wisdomColor by ColorSetting("Wisdom Color", Color(mc.fontRendererObj.getColorCode('b'))).hideIf { ! wisdom }
    private val lifeColor by ColorSetting("Life Color", Color(mc.fontRendererObj.getColorCode('c'))).hideIf { ! life }
    private val stoneColor by ColorSetting("Stone Color", Color(mc.fontRendererObj.getColorCode('7'))).hideIf { ! stone }

    private data class BlessingData(val type: Blessing, val enabled: () -> Boolean, val color: () -> Color)

    private val blessings = listOf(
        BlessingData(Blessing.POWER, { power }, { powerColor }),
        BlessingData(Blessing.TIME, { time }, { timeColor }),
        BlessingData(Blessing.STONE, { stone }, { stoneColor }),
        BlessingData(Blessing.LIFE, { life }, { lifeColor }),
        BlessingData(Blessing.WISDOM, { wisdom }, { wisdomColor })
    )

    object BlessingDisplayElement: GuiElement(hudData.getData().blessingDisplay) {
        private var text: List<String> = listOf()
        override val enabled get() = BlessingDisplay.enabled
        override val width get() = text.takeIf { it.isNotEmpty() }?.maxOf(RenderHelper::getStringWidth) ?: 10f
        override val height get() = RenderHelper.getStringHeight(text).takeIf { it > 0 } ?: 10f

        override fun draw() {
            if (HudEditorScreen.isOpen()) return
            blessings.filter { it.enabled.invoke() && it.type.current > 0 }.forEachIndexed { index, blessing ->
                val str = "${blessing.type.displayString} &f${blessing.type.current}"
                RenderUtils.drawText(str, getX(), getY() + index * 9 * getScale(), getScale(), blessing.color.invoke())
            }
        }

        override fun exampleDraw() {
            text = blessings.filter { it.enabled.invoke() }.map { blessing ->
                "${blessing.type.displayString} 5"
            }

            blessings.filter { it.enabled.invoke() }.forEachIndexed { index, blessing ->
                val str = "${blessing.type.displayString} 5"
                RenderUtils.drawText(str, getX(), getY() + index * 9 * getScale(), getScale(), blessing.color.invoke())
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! BlessingDisplayElement.enabled) return
        if (! inDungeon) return
        BlessingDisplayElement.draw()
    }
}
