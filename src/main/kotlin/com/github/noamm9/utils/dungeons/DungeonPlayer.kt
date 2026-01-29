package com.github.noamm9.utils.dungeons

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.dungeons.enums.Classes
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.dungeons.map.utils.MapUtils
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3

data class DungeonPlayer(
    var name: String,
    var clazz: Classes,
    var clazzLvl: Int,
    var skin: ResourceLocation = mc.player !!.skin.body.texturePath(),
    var isDead: Boolean = false,
) {
    var entity: AbstractClientPlayer? = null
        set(value) {
            skin = value?.skin?.body?.texturePath() ?: skin
            field = value
        }

    var mapX = 0f
    var mapZ = 0f
    var yaw = 0f
    var icon = ""

    fun getRealPos() = Vec3(
        (mapX - MapUtils.startCorner.first) / MapUtils.coordMultiplier + DungeonScanner.startX - 15,
        entity?.y ?: .0,
        (mapZ - MapUtils.startCorner.second) / MapUtils.coordMultiplier + DungeonScanner.startZ - 15
    )

    val clearedRooms: Pair<MutableSet<String>, MutableSet<String>> = mutableSetOf<String>() to mutableSetOf()
    val deaths: MutableList<String> = mutableListOf()
    var secretsBeforeRun: Long = 0

    companion object {
        fun get(name: String) = DungeonListener.dungeonTeammates.find { it.name == name } ?: DungeonListener.thePlayer
    }
}