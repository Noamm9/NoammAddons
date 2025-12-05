package noammaddons.commands.commands

import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import noammaddons.commands.Command
import noammaddons.features.impl.dungeons.waypoints.DungeonWaypoints
import noammaddons.features.impl.dungeons.waypoints.WaypointEditorGui
import noammaddons.utils.*

object DungeonWaypointCommand: Command("dw", aliases = listOf("dungeonwaypoint", "dungeonwaypoints"), usage = "/dw <add|edit|remove|clear>") {
    override fun processCommand(sender: ICommandSender, args: Array<out String>) {
        if (args.isEmpty()) return ChatUtils.modMessage("§cUsage: " + getCommandUsage(sender))
        val command = args[0].lowercase()

        val roomName: String
        val roomCenter: BlockPos
        val rotation: Int

        if (LocationUtils.dungeonFloorNumber == null) return ChatUtils.modMessage("§cYou must be in a dungeon to edit waypoints!")
        if (! LocationUtils.inBoss) {
            val currentRoom = ScanUtils.currentRoom ?: return ChatUtils.modMessage("§cYou must be in a dungeon room to edit waypoints!")
            roomName = currentRoom.data.name
            roomCenter = ScanUtils.getRoomCenter(currentRoom)
            rotation = currentRoom.rotation ?: return
        }
        else {
            roomName = "B" + LocationUtils.dungeonFloorNumber
            roomCenter = BlockPos(0, 0, 0)
            rotation = 0
        }

        when (command) {
            "add" -> {
                val lookingAt = mc.objectMouseOver?.takeIf { it.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK }?.blockPos ?: return ChatUtils.modMessage("§cYou must be looking at a block!")
                if (DungeonWaypoints.currentRoomWaypoints.any { it.pos == lookingAt }) return ChatUtils.modMessage("§cA waypoint already exists here. Use /dw edit.")
                val relativePos = ScanUtils.getRelativeCoord(lookingAt, roomCenter, rotation)
                GuiUtils.openScreen(WaypointEditorGui(roomName, lookingAt, relativePos))
            }

            "edit" -> {
                val lookingAt = mc.objectMouseOver?.takeIf { it.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK }?.blockPos ?: return ChatUtils.modMessage("§cYou must be looking at a block!")
                val existing = DungeonWaypoints.currentRoomWaypoints.firstOrNull { it.pos == lookingAt } ?: return ChatUtils.modMessage("§cNo waypoint found at that block.")
                val relativePos = ScanUtils.getRelativeCoord(lookingAt, roomCenter, rotation)
                GuiUtils.openScreen(WaypointEditorGui(roomName, lookingAt, relativePos, existing))
            }

            "remove" -> {
                val playerPos = mc.thePlayer.position
                val closest = DungeonWaypoints.currentRoomWaypoints.minByOrNull { it.pos.distanceSq(playerPos) }

                if (closest != null && closest.pos.distanceSq(playerPos) < 25.0) {
                    val relativePosToRemove = ScanUtils.getRelativeCoord(closest.pos, roomCenter, rotation)

                    val currentData = DungeonWaypoints.waypoints
                    val roomList = currentData.getOrDefault(roomName, emptyList()).toMutableList()

                    if (roomList.removeIf { it.pos == relativePosToRemove }) {
                        DungeonWaypoints.waypoints[roomName] = roomList
                        DungeonWaypoints.saveConfig()
                        DungeonWaypoints.currentRoomWaypoints.remove(closest)
                        ChatUtils.modMessage("§aWaypoint removed.")
                    }
                    else ChatUtils.modMessage("§cError syncing config.")

                }
                else ChatUtils.modMessage("§cNo waypoint found nearby.")
            }

            "clear" -> {
                if (DungeonWaypoints.currentRoomWaypoints.isEmpty()) return ChatUtils.modMessage("§cNo waypoints set for this room.")
                DungeonWaypoints.waypoints.remove(roomName)
                DungeonWaypoints.saveConfig()
                DungeonWaypoints.currentRoomWaypoints.clear()
                ChatUtils.modMessage("§aAll waypoints cleared.")
            }

            else -> ChatUtils.modMessage("§cUnknown subcommand. Usage: " + getCommandUsage(sender))
        }
    }

    override fun addTabCompletionOptions(
        sender: ICommandSender?,
        args: Array<out String>,
        pos: BlockPos?
    ): List<String> {
        if (args.size == 1) return getListOfStringsMatchingLastWord(args, "add", "edit", "remove", "clear")
        return emptyList()
    }
}