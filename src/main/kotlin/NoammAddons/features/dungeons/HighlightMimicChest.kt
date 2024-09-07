package NoammAddons.features.dungeons

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.LocationUtils.inDungeons
import NoammAddons.utils.RenderUtils
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

object HighlightMimicChest {
    @SubscribeEvent
    fun renderMimicChest(event: RenderWorldLastEvent) {
        if (!inDungeons || !config.HightlightMimicChest) return
        val PossibleMimicChests = mc.theWorld.loadedTileEntityList.filter{it is TileEntityChest && it.chestType == 1}.map {it.pos}
        if (PossibleMimicChests.isEmpty()) return
        PossibleMimicChests.forEach {
            RenderUtils.drawBlockBox(it, Color(255, 60, 60, 85), true, true, true, 2f)
            RenderUtils.drawString("Mimic", Vec3(it.x + 0.5, (it.y + 2).toDouble(), it.z + 0.5), Color(255, 60, 60), 2f)
        }
    }
}
