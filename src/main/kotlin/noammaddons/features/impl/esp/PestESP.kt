package noammaddons.features.impl.esp

import gg.essential.elementa.utils.withAlpha
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Items
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.*
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.RenderHelper.renderVec
import java.awt.Color

object PestESP: Feature("Highlight Pests in the Garden") {
    private val pestList = DataDownloader.loadJson<Map<String, String>>("PestTextures.json").entries.associate { it.value to it.key }

    private val box = ToggleSetting("Box")
    private val trace = ToggleSetting("Tracer")
    private val boxColor = ColorSetting("Box Color", Color.CYAN.withAlpha(50))
    private val traceColor = ColorSetting("Tracer Color", Color.CYAN, false)
    override fun init() = addSettings(box, trace, SeperatorSetting("Colors"), boxColor, traceColor)

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (LocationUtils.world != LocationUtils.WorldType.Garden) return

        for (entity in mc.theWorld.loadedEntityList.filterIsInstance<EntityArmorStand>()) {
            val armorItemStack = entity.getCurrentArmor(3) ?: continue
            if (armorItemStack.item !== Items.skull) continue
            val texture = ItemUtils.getSkullTexture(armorItemStack)
            if (texture !in pestList) continue

            val pestType = pestList[texture] ?: continue
            if (pestType == "WormAss") continue
            val (x, y, z) = entity.renderVec.destructured()
            val Yoffset = if (pestType == "Worm") 0.5 else 1.3
            val XZoffset = 0.5

            if (box.value) RenderUtils.drawBox(
                x - XZoffset, y + Yoffset, z - XZoffset,
                boxColor.value, outline = true, fill = true,
                width = 1, height = 1, phase = true,
                lineWidth = 1.5
            )

            if (trace.value) RenderUtils.drawTracer(
                entity.renderVec.add(y = Yoffset + 0.5),
                traceColor.value, lineWidth = 1.5f
            )
        }
    }
}
