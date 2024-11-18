package noammaddons.features.dungeons

import net.minecraft.tileentity.TileEntityChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderChestEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.RenderHelper.disableChums
import noammaddons.utils.RenderHelper.enableChums
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.RenderUtils.drawString
import java.awt.Color

object HighlightMimicChest: Feature() {
    @SubscribeEvent
    fun renderMimicChest(event: RenderWorld) {
        if (! check()) return

        mc.theWorld.loadedTileEntityList.filter {
            it is TileEntityChest && it.chestType == 1
        }.forEach {

            drawBlockBox(
                it.pos, Color(255, 60, 60, 40),
                outline = true, fill = true,
                phase = true, LineThickness = 2f
            )

            drawString(
                "Mimic",
                it.pos.x + 0.5,
                it.pos.y + 2,
                it.pos.z + 0.5,
                Color(255, 60, 60),
                1.5f, renderBlackBox = false,
                shadow = true, phase = true
            )
        }
    }

    @SubscribeEvent
    fun aa(event: RenderChestEvent.Pre) {
        if (! check()) return
        if (event.chest.chestType != 1) return

        enableChums(Color.WHITE)
    }

    @SubscribeEvent
    fun a2a(event: RenderChestEvent.Post) {
        if (! check()) return
        if (event.chest.chestType != 1) return

        disableChums()
    }

    fun check(): Boolean = config.HighlightMimicChest && inDungeons && ! inBoss
}
