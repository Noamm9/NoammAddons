package noammaddons.utils.ScanUtils

import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.JsonUtils.fetchJsonWithRetry
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.ScanUtils.Utils.getCore
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.Utils.isNull
import kotlin.math.floor


object ScanRoom {
    private var roomData: List<Room>? = null

    @JvmField
    var currentRoom: Room? = null

    @JvmField
    var lastKnownRoom: Room? = null

    init {
        fetchJsonWithRetry<List<Room>?>("https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/roomdata.json") {
            roomData = it
        }

        loop(1000) {
            currentRoom = when {
                Player.isNull() || mc.theWorld.isNull() -> null
                config.DevMode -> getRoom()
                inDungeons && ! inBoss -> getRoom()
                else -> null
            }

            if (currentRoom != null) {
                lastKnownRoom = currentRoom
            }
        }

    }


    /**
     * Maps real world coords to 5x5 grind depending on where they are in the dungeon.
     */
    fun getRoomComponent(realX: Int = Player !!.posX.toInt(), realZ: Int = Player !!.posZ.toInt()): Coords2D {
        return Coords2D(
            floor((realX + 200 + 0.5) / 32).toInt(),
            floor((realZ + 200 + 0.5) / 32).toInt()
        )
    }


    fun getRoomCorner(x: Int = Player !!.posX.toInt(), z: Int = Player !!.posZ.toInt()): Coords2D {
        val roomComponent = getRoomComponent(x, z)

        return Coords2D(
            - 200 + roomComponent.x * 32,
            - 200 + roomComponent.z * 32
        )
    }

    fun getRoomCenter(x: Int = Player !!.posX.toInt(), z: Int = Player !!.posZ.toInt()): Coords2D {
        val RoomCorner = getRoomCorner(x, z)

        return Coords2D(
            RoomCorner.x + 15,
            RoomCorner.z + 15
        )
    }


    fun getRoom(X: Int = Player !!.posX.toInt(), Z: Int = Player !!.posZ.toInt()): Room? {
        if (roomData.isNull()) {
            debugMessage("&4[ERROR]&r Failed to fetch RoomData!")
            return null
        }
        else {
            val roomComponent = getRoomCenter(X, Z)
            return roomData !!.find { it.cores.contains(getCore(roomComponent.x, roomComponent.z)) }
        }
    }
}

// TODO: Rotation scanning