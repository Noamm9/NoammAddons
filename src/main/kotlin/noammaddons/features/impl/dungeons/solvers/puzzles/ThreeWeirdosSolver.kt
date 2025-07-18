package noammaddons.features.impl.dungeons.solvers.puzzles

import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.mc
import noammaddons.NoammAddons.Companion.personalBests
import noammaddons.events.*
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.Wcolor
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.WcolorWrong
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.WremoveChests
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.WremoveNPCS
import noammaddons.utils.BlockUtils.ghostBlock
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.DungeonUtils
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.ScanUtils.rotate
import noammaddons.utils.Utils.formatPbPuzzleMessage
import java.util.concurrent.CopyOnWriteArraySet


object ThreeWeirdosSolver {
    private val npcRegex = Regex("\\[NPC] (\\w+): (.+)")

    private var inWeirdos = false
    private var rotation = 0

    private var trueTimeStart: Long? = null
    private var timeStart: Long? = null

    private var correctPos: Pair<BlockPos, Entity?>? = null
    private var wrongPositions = CopyOnWriteArraySet<Pair<BlockPos, Entity>>()

    @SubscribeEvent
    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (event.room.data.name != "Three Weirdos") return
        inWeirdos = true

        rotation = 360 - event.room.rotation !!
        trueTimeStart = System.currentTimeMillis()

        debugMessage("Three Weirdos: Rotation: $rotation")
    }

    @SubscribeEvent
    fun onRoomExit(event: DungeonEvent.RoomEvent.onExit) = reset()

    @SubscribeEvent
    fun onPazzleEvent(event: DungeonEvent.PuzzleEvent.Reset) {
        if (! inWeirdos) return
        wrongPositions.forEach {
            if (WremoveNPCS.value) mc.theWorld.addEntityToWorld(it.second.entityId, it.second)
            if (WremoveChests.value) ghostBlock(it.first, Blocks.chest.defaultState)
        }
        reset()
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! inWeirdos) return
        correctPos?.first?.let { chest ->
            drawBlockBox(
                chest, Wcolor.value, phase = false,
                fill = true, outline = true
            )
        }
        if (! WremoveChests.value) wrongPositions.forEach { (pos, _) ->
            drawBlockBox(
                pos, WcolorWrong.value, phase = false,
                fill = true, outline = true
            )
        }
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! inWeirdos) return
        npcRegex.find(event.component.noFormatText)?.let { match ->
            val (npc, msg) = match.destructured
            if (solutions.none { it.matches(msg) } && wrong.none { it.matches(msg) }) return
            val correctNPC = mc.theWorld?.loadedEntityList?.find { it is EntityArmorStand && it.name.removeFormatting() == npc } ?: return
            val offset = BlockPos(1, 0, 0).rotate(rotation)
            val pos = BlockPos(correctNPC.posX - 0.5, 69.0, correctNPC.posZ - 0.5).add(offset)

            if (solutions.any { it.matches(msg) }) {
                mc.thePlayer.playSound("note.pling", 2f, 1f)
                timeStart = System.currentTimeMillis()
                correctPos = pos to correctNPC
                if (! WremoveNPCS.value) return

                val entitiesToRemove = mc.theWorld.getEntitiesInAABBexcluding(
                    mc.thePlayer, correctNPC.entityBoundingBox.offset(0.0, - 1.0, 0.0)
                ) { it !in DungeonUtils.dungeonTeammates.map { teammate -> teammate.entity } }

                entitiesToRemove.forEach { it.setDead() }
            }
            else {
                val entitiesToRemove = mc.theWorld.getEntitiesInAABBexcluding(
                    mc.thePlayer, correctNPC.entityBoundingBox.offset(0.0, - 1.0, 0.0)
                ) { it !in DungeonUtils.dungeonTeammates.map { teammate -> teammate.entity } }

                if (WremoveNPCS.value) entitiesToRemove.forEach { it.setDead() }
                if (WremoveChests.value) mc.theWorld.setBlockToAir(pos)
                wrongPositions.add(pos to correctNPC)
            }
        }
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Sent) {
        if (! inWeirdos || wrongPositions.size != 2) return
        val packet = event.packet as? C08PacketPlayerBlockPlacement ?: return
        if (packet.position != correctPos?.first) return

        val personalBestsData = personalBests.getData().pazzles
        val previousBest = personalBestsData["Three Weirdos"]
        val completionTime = (System.currentTimeMillis() - timeStart !!).toDouble()
        val totalTime = (System.currentTimeMillis() - trueTimeStart !!).toDouble()

        val message = formatPbPuzzleMessage("Three Weirdos", completionTime, previousBest)

        sendPartyMessage(message)

        clickableChat(
            msg = message,
            cmd = "/na copy ${message.removeFormatting()}",
            hover = "Total Time: &b${(totalTime / 1000.0).toFixed(2)}s",
            prefix = false
        )

        reset()
    }


    fun reset() {
        inWeirdos = false
        rotation = 0
        trueTimeStart = null
        timeStart = null
        correctPos = null
        wrongPositions.clear()
    }

    private val solutions = listOf(
        Regex("The reward is not in my chest!"),
        Regex("At least one of them is lying, and the reward is not in .+'s chest.?"),
        Regex("My chest doesn't have the reward. We are all telling the truth.?"),
        Regex("My chest has the reward and I'm telling the truth!"),
        Regex("The reward isn't in any of our chests.?"),
        Regex("Both of them are telling the truth. Also, .+ has the reward in their chest.?"),
    )

    private val wrong = listOf(
        Regex("One of us is telling the truth!"),
        Regex("They are both telling the truth. The reward isn't in .+'s chest."),
        Regex("We are all telling the truth!"),
        Regex(".+ is telling the truth and the reward is in his chest."),
        Regex("My chest doesn't have the reward. At least one of the others is telling the truth!"),
        Regex("One of the others is lying."),
        Regex("They are both telling the truth, the reward is in .+'s chest."),
        Regex("They are both lying, the reward is in my chest!"),
        Regex("The reward is in my chest."),
        Regex("The reward is not in my chest. They are both lying."),
        Regex(".+ is telling the truth."),
        Regex("My chest has the reward.")
    )
}