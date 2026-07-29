package com.github.noamm9.utils.dungeons.map.core

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.features.impl.dungeon.map.MapConfig
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.map.core.RoomType.*
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import kotlin.properties.Delegates

class RoomTile(override val x: Int, override val z: Int, var data: RoomData): Tile {
    var uniqueRoom: UniqueRoom? = null
    var isSeparator = false

    override var state by Delegates.observable(RoomState.UNDISCOVERED) { _, oldValue, newValue ->
        if (uniqueRoom?.mainRoom != this) return@observable
        if (oldValue == newValue) return@observable
        if (data.name == "Unknown") return@observable
        if (MapConfig.dungeonMapCheater.value && oldValue == RoomState.UNOPENED && newValue == RoomState.UNDISCOVERED) return@observable
        if (MapConfig.dungeonMapCheater.value && newValue == RoomState.UNOPENED && oldValue == RoomState.UNDISCOVERED) return@observable

        val roomPlayers = DungeonListener.dungeonTeammates.filter {
            val pos = if (it.entity == mc.player) mc.player !!.position() else it.getRealPos()
            ScanUtils.getRoomFromPos(pos)?.data?.name == data.name
        }

        if (newValue == RoomState.GREEN) uniqueRoom !!.foundSecrets = uniqueRoom !!.data.secrets
        EventBus.post(DungeonEvent.RoomEvent.onStateChange(uniqueRoom !!, oldValue, newValue, roomPlayers))
    }

    override fun getColor() = when {
        state == RoomState.UNOPENED -> MapConfig.colorUnopened
        data.type == BLOOD -> MapConfig.colorBlood
        data.type == FAIRY -> MapConfig.colorFairy
        data.type == RARE -> MapConfig.colorRare
        data.type == CHAMPION -> MapConfig.colorMiniboss
        data.type == PUZZLE -> MapConfig.colorPuzzle
        data.type == TRAP -> MapConfig.colorTrap
        data.type == NORMAL -> MapConfig.colorRoom
        data.type == ENTRANCE -> MapConfig.colorEntrance
        else -> MapConfig.colorRoom
    }.value


    fun addToUnique(row: Int, column: Int, roomName: String = data.name) {
        val unique = DungeonScanner.uniqueRooms[roomName]

        if (unique == null) {
            UniqueRoom(column, row, this).let {
                DungeonScanner.uniqueRooms[data.name] = it
                uniqueRoom = it
            }
        }
        else {
            unique.addTile(column, row, this)
            uniqueRoom = unique
        }
    }
}