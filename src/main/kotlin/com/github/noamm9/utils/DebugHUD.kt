package com.github.noamm9.utils

import com.github.noamm9.NoammAddons
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.Blessing
import com.github.noamm9.utils.dungeons.enums.Puzzle
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

object DebugHUD {
    @JvmStatic
    fun render(guiGraphics: GuiGraphics) {
        renderDungeonDebug(guiGraphics)
        renderLocationDebug(guiGraphics)
        renderPartyDebug(guiGraphics)
    }


    private fun renderDungeonDebug(graphics: GuiGraphics) {
        if (! NoammAddons.debugFlags.contains("dungeon")) return

        var y = 20
        val x = 10

        fun draw(text: String, color: Int = 0xFFFFFF) {
            Render2D.drawString(graphics, text, x, y, color = Color(color))
            y += 10
        }

        draw("§6§lDUNGEON DEBUGGER", 0xFFAA00)
        draw("Dungeon Started: ${if (DungeonListener.dungeonStarted) "§aYES" else "§cNO"}")
        draw("Dungeon Ended: ${if (DungeonListener.dungeonEnded) "§aYES" else "§cNO"}")
        draw("Current Tick: §7${DungeonListener.currentTime} §8(${DungeonListener.currentTime / 20}s)")
        draw("Door Keys: §e${DungeonListener.doorKeys}")
        draw("Last Opener: §d${DungeonListener.lastDoorOpenner?.name ?: "None"}")

        y += 5

        draw("§c§lTimestamps (Ticks)", 0xFF5555)
        fun formatTS(name: String, time: Long?) {
            val display = time?.let { "§f${it}t §8(${it / 20}s)" } ?: "§7N/A"
            draw("$name: $display")
        }
        formatTS("Start", DungeonListener.dungeonStartTime)
        formatTS("Blood Open", DungeonListener.bloodOpenTime)
        formatTS("Watcher Spawn", DungeonListener.watcherFinishSpawnTime)
        formatTS("Watcher Clear", DungeonListener.watcherClearTime)
        formatTS("Boss Entry", DungeonListener.bossEntryTime)
        formatTS("Run End", DungeonListener.dungeonEndTime)

        y += 5

        draw("§b§lTEAMMATES (${DungeonListener.dungeonTeammates.size})", 0x55FFFF)
        if (DungeonListener.dungeonTeammates.isEmpty()) draw(" §7No teammates detected...")
        else DungeonListener.dungeonTeammates.forEach { player ->
            val status = if (player.isDead) "§c[DEAD]" else "§a[ALIVE]"
            val isSelf = if (player == DungeonListener.thePlayer) "§d(YOU)" else ""
            draw("§f${player.name} §7- ${player.clazz.name} ${player.clazzLvl} $status ${player.icon} $isSelf")
        }

        y += 5

        val activePuzzles = DungeonListener.puzzles.count { it != Puzzle.UNKNOWN }
        draw("§d§lPUZZLES ($activePuzzles/${DungeonListener.maxPuzzleCount})", 0xFF55FF)
        if (DungeonListener.puzzles.isEmpty()) draw(" §7No puzzles found yet...")
        else DungeonListener.puzzles.forEachIndexed { index, puzzle ->
            val pColor = when (puzzle.state) {
                RoomState.GREEN, RoomState.CLEARED -> "§a"
                RoomState.FAILED -> "§c"
                RoomState.DISCOVERED -> "§e"
                else -> "§7"
            }

            val name = if (puzzle == Puzzle.UNKNOWN) "Slot $index" else puzzle.name
            draw(" §7- $pColor$name §8[§7${puzzle.state}§8]")
        }

        y += 5

        draw("§a§lBLESSINGS", 0x55FF55)
        var foundBlessing = false
        Blessing.entries.forEach { blessing ->
            if (blessing.current > 0) {
                draw(" §f${blessing.name}: §a${blessing.current}")
                foundBlessing = true
            }
        }
        if (! foundBlessing) draw(" §7No blessings active")
    }

    private fun renderLocationDebug(graphics: GuiGraphics) {
        if (! NoammAddons.debugFlags.contains("location")) return

        var y = 20
        val x = 180

        fun draw(text: String, color: Int = 0xFFFFFF) {
            Render2D.drawString(graphics, text, x, y, color = Color(color))
            y += 10
        }

        draw("§b§lLOCATION DEBUGGER", 0x55FFFF)
        draw("On Hypixel: ${if (LocationUtils.onHypixel) "§aYES" else "§cNO"}")
        draw("In Skyblock: ${if (LocationUtils.inSkyblock) "§aYES" else "§cNO"}")
        draw("World Type: §e${LocationUtils.world?.name ?: "None"}")

        y += 5

        draw("§d§lDUNGEON STATE", 0xFF55FF)
        draw("In Dungeon: ${if (LocationUtils.inDungeon) "§aYES" else "§cNO"}")
        draw("Floor: §f${LocationUtils.dungeonFloor ?: "N/A"} §8(Num: ${LocationUtils.dungeonFloorNumber ?: "None"})")
        draw("Master Mode: ${if (LocationUtils.isMasterMode) "§aYES" else "§cNO"}")
        draw("In Boss Room: ${if (LocationUtils.inBoss) "§cYES" else "§cNO"}")

        y += 5
        draw("§c§lFLOOR 7 / M7 DATA", 0xFF5555)

        val phase = LocationUtils.F7Phase
        val phaseText = when (phase) {
            1 -> "§bP1 (Maxor)"
            2 -> "§6P2 (Storm)"
            3 -> "§eP3 (Goldor/Terminals)"
            4 -> "§cP4 (Necron)"
            5 -> "§dP5 (Dragons)"
            else -> "§7Unknown"
        }
        draw("Phase: $phaseText")

        if (phase == 3) {
            val section = LocationUtils.P3Section
            val sectionName = when (section) {
                1 -> "§a1"
                2 -> "§92"
                3 -> "§53"
                4 -> "§64"
                else -> "§7Searching..."
            }
            draw("P3 Section: $sectionName")
        }

    }

    private fun renderPartyDebug(graphics: GuiGraphics) {
        if (! NoammAddons.debugFlags.contains("party")) return
        var y = 20
        val x = 350

        fun draw(text: String, color: Int = 0xFFFFFF) {
            Render2D.drawString(graphics, text, x, y, color = Color(color))
            y += 10
        }

        draw("§d§lPARTY DEBUGGER", 0xFF55FF)
        draw("In Party: ${if (PartyUtils.isInParty) "§aYES" else "§cNO"}")
        draw("Is Leader: ${if (PartyUtils.isLeader()) "§aYES" else "§cNO"}")

        val leaderName = PartyUtils.partyLeader
        val selfName = NoammAddons.mc.player?.gameProfile?.name
        draw("Party Leader: ${leaderName?.let { "§f$it" } ?: "§7None"}")

        y += 5
        draw("§b§lMEMBERS (${PartyUtils.members.size})", 0x55FFFF)
        if (PartyUtils.members.isEmpty()) draw(" §7No members detected...")
        else PartyUtils.members.forEach { member ->
            val tags = buildList {
                if (member == leaderName) add("§6[LEADER]")
                if (member == selfName) add("§d(YOU)")
            }.joinToString(" ")

            val tagText = if (tags.isNotEmpty()) " $tags" else ""
            draw(" §f$member$tagText")
        }
    }
}