package noammaddons.features.dungeons

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.RenderUtils
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.utils.LocationUtils.inBoss
import java.awt.Color

object HighlightMimicChest {
    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun renderMimicChest(event: RenderWorldLastEvent) {
        if (!inDungeons || !config.HighlightMimicChest || inBoss) return
        val PossibleMimicChests = mc.theWorld.loadedTileEntityList.filter{it is TileEntityChest && it.chestType == 1}.map {it.pos}
        if (PossibleMimicChests.isEmpty()) return
        PossibleMimicChests.forEach {
            RenderUtils.drawBlockBox(
	            it,
	            Color(255, 60, 60, 85),
	            outline = true,
	            fill = true,
	            phase = true,
	            LineThickness = 2f
            )
            RenderUtils.drawString(
	            "Mimic",
	            it.x + 0.5,
	            it.y + 2,
	            it.z + 0.5,
	            Color(255, 60, 60),
	            2f
			)
        }
    }
}
