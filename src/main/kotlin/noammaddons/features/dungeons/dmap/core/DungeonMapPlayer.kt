package noammaddons.features.dungeons.dmap.core

import net.minecraft.util.ResourceLocation
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
}
