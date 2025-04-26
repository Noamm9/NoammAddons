package noammaddons.features.impl.dungeons.dmap.core

import noammaddons.utils.DungeonUtils

class ClearInfo {
    val clearedRooms: Pair<MutableSet<String>, MutableSet<String>> = mutableSetOf<String>() to mutableSetOf()
    val deaths: MutableList<String> = mutableListOf()
    var secretsBeforeRun: Int = 0

    companion object {
        fun get(name: String) = DungeonUtils.dungeonTeammates.first {
            it.name == name
        }.clearInfo
    }
}