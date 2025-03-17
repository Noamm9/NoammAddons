package noammaddons.features.general.PestESP

import gg.essential.elementa.utils.withAlpha
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Items
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.*
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.RenderHelper.renderVec

object PestESP: Feature() {
    private val pestList = mutableMapOf<String, String>()

    init {
        JsonUtils.fetchJsonWithRetry<Map<String, String>?>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/PestTextures.json"
        ) {
            it ?: return@fetchJsonWithRetry
            for ((k, v) in it) {
                pestList[v] = k
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! config.pestEsp) return
        if (LocationUtils.world != LocationUtils.WorldType.Garden) return

        for (entity in mc.theWorld.loadedEntityList.filterIsInstance<EntityArmorStand>()) {
            val armorItemStack = entity.getCurrentArmor(3) ?: continue
            if (armorItemStack.item !== Items.skull) continue
            val texture = ItemUtils.getSkullTexture(armorItemStack)
            if (texture !in pestList) continue

            val pestType = pestList[texture] ?: continue
            if (pestType == "WormAss") continue
            val color = config.pestEspColor.withAlpha(80)
            val (x, y, z) = entity.renderVec.destructured()
            val Yoffset = if (pestType == "Worm") 0.5 else 1.3
            val XZoffset = 0.5

            RenderUtils.drawBox(
                x - XZoffset,
                y + Yoffset,
                z - XZoffset,
                color, outline = true, fill = true,
                width = 1, height = 1, phase = true,
                LineThickness = 1.5
            )

            RenderUtils.drawTracer(
                entity.renderVec.add(y = Yoffset + 0.5),
                color = color, lineWidth = 1.5f
            )
        }

    }
}
