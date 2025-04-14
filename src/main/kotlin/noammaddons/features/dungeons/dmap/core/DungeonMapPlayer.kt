package noammaddons.features.dungeons.dmap.core

import net.minecraft.util.BlockPos
import net.minecraft.util.ResourceLocation
import noammaddons.features.dungeons.dmap.handlers.DungeonScanner
import noammaddons.features.dungeons.dmap.utils.MapUtils.coordMultiplier
import noammaddons.features.dungeons.dmap.utils.MapUtils.startCorner
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.DungeonUtils.DungeonPlayer


data class DungeonMapPlayer(val teammate: DungeonPlayer, val skin: ResourceLocation) {
    companion object {
        val dummy
            get() = DungeonPlayer(
                mc.session.username,
                Empty, 55, mc.thePlayer.locationSkin,
                mc.thePlayer, false
            )
    }

    var mapX = 0f
    var mapZ = 0f
    var yaw = 0f
    var icon = ""

    fun getRealPos() = BlockPos(
        (mapX - startCorner.first) / coordMultiplier + DungeonScanner.startX - 15,
        0.0,
        (mapZ - startCorner.second) / coordMultiplier + DungeonScanner.startZ - 15
    )
}
