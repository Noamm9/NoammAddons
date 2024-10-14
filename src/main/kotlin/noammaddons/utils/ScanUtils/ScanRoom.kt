package noammaddons.utils.ScanUtils

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderOverlay
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.JsonUtils.fetchJsonWithRetry
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ScanUtils.Utils.getCore
import noammaddons.utils.ThreadUtils.runEvery
import noammaddons.utils.Utils.isNull
import java.awt.Color
import kotlin.math.floor


object ScanRoom {
	private var roomData: List<Room>? = null
	var currentRoom: Room? = null
	
	init {
		fetchJsonWithRetry<List<Room>?>("https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/roomdata.json") {
			roomData = it
		}
		
		runEvery(1000) {
			if (Player.isNull () || mc.theWorld.isNull() ||
			    !config.DevMode && (!inDungeons || inBoss)
			) return@runEvery
			
			currentRoom = getRoom() ?: return@runEvery
		}
	}
	
	
	/**
	 * Maps real world coords to 5x5 grind depending on where they are in the dungeon.
	*/
	fun getRoomComponent(realX: Int = Player!!.posX.toInt(), realZ: Int = Player!!.posZ.toInt()): Coords2D {
		return Coords2D(
			floor((realX + 200 + 0.5) / 32).toInt(),
			floor((realZ + 200 + 0.5) / 32).toInt()
		)
	}
	
	
	fun getRoomCorner(x: Int = Player!!.posX.toInt(), z: Int = Player!!.posZ.toInt()): Coords2D {
		val roomComponent = getRoomComponent(x, z)
		
		return Coords2D(
			-200 + roomComponent.x * 32,
			-200 + roomComponent.z * 32
		)
	}
	
	fun getRoomCenter(x: Int = Player!!.posX.toInt(), z: Int = Player!!.posZ.toInt()): Coords2D {
		val RoomCorner = getRoomCorner(x, z)
		
		return Coords2D(
			RoomCorner.x + 15,
			RoomCorner.z + 15
		)
	}
	
	
	fun getRoom(X: Int = Player!!.posX.toInt(), Z: Int = Player!!.posZ.toInt()): Room? {
		if (roomData.isNull()) {
			debugMessage("&4[ERROR]&r Failed to fetch RoomData!")
			return null
		}
		else {
			val roomComponent = getRoomCenter(X, Z)
			return roomData!!.find { it.cores.contains(getCore(roomComponent.x, roomComponent.z)) }
		}
	}
	
	fun getDungeonsPosIndex(): Int {
		val roomComponent = getRoomComponent()
		return roomComponent.x * 6 + roomComponent.z
	}
}