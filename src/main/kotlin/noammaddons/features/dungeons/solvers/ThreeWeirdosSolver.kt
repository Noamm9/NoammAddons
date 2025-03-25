package noammaddons.features.dungeons.solvers

import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.personalBests
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.DungeonUtils
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderUtils
import noammaddons.utils.ScanUtils.getRoomCenterAt
import noammaddons.utils.ScanUtils.getRotation
import noammaddons.utils.ScanUtils.rotateCoords
import noammaddons.utils.Utils.formatPbPuzzleMessage
import java.util.concurrent.CopyOnWriteArraySet


object ThreeWeirdosSolver: Feature() {
    private val npcRegex = Regex("\\[NPC] (\\w+): (.+)")

    private var inWeirdos = false
    private var rotation = 0

    private var trueTimeStart: Long? = null
    private var timeStart: Long? = null

    private var correctPos: Pair<BlockPos, Entity?>? = null
    private var wrongPositions = CopyOnWriteArraySet<Pair<BlockPos, Entity>>()

    private val chestsPos = mapOf(
        Blocks.redstone_wire to listOf(7, 69, 2),
        Blocks.chest as Block to listOf(9, 69, 1),
        Blocks.chest as Block to listOf(10, 69, - 1),
        Blocks.chest as Block to listOf(9, 69, - 3)
    )

    @SubscribeEvent
    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (! config.ThreeWeirdosSolver) return
        if (event.room.name != "Three Weirdos") return

        val center = getRoomCenterAt(mc.thePlayer.position)
        val detectedRotation = getRotation(center, chestsPos) ?: return

        inWeirdos = true
        rotation = detectedRotation * 90
        trueTimeStart = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onRoomExit(event: DungeonEvent.RoomEvent.onExit) = reset()


    @SubscribeEvent
    fun onChat(event: Chat) {
        val (npc, msg) = npcRegex.find(event.component.noFormatText)?.destructured ?: return
        if (solutions.none { it.matches(msg) } && wrong.none { it.matches(msg) }) return
        val correctNPC = mc.theWorld?.loadedEntityList?.find { it is EntityArmorStand && it.name.removeFormatting() == npc } ?: return
        val offset = rotateCoords(listOf(0, 0, - 1), rotation).let { BlockPos(it[0], it[1], it[2]) }
        val pos = BlockPos(correctNPC.posX - 0.5, 69.0, correctNPC.posZ - 0.5).add(offset)

        if (solutions.any { it.matches(msg) }) {
            mc.thePlayer.playSound("note.pling", 2f, 1f)
            correctPos = pos to correctNPC
        }
        else wrongPositions.add(pos to correctNPC)

        if (correctPos == null) return
        timeStart = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! inWeirdos) return

        correctPos?.let { (chest, entity) ->
            RenderUtils.drawBlockBox(
                chest, config.ThreeWeirdosSolverColor,
                fill = true, outline = true, phase = true
            )

            if (entity == null) return@let
            val entitiesToRemove = mc.theWorld.getEntitiesInAABBexcluding(
                mc.thePlayer, entity.entityBoundingBox.offset(0.0, - 1.0, 0.0)
            ) { it !in DungeonUtils.dungeonTeammates.map { teammate -> teammate.entity } }

            entitiesToRemove.forEach { it.setDead() }
            correctPos = chest to null
        }

        wrongPositions.forEach { (chest, entity) ->
            val entitiesToRemove = mc.theWorld.getEntitiesInAABBexcluding(
                mc.thePlayer, entity.entityBoundingBox.offset(0.0, - 1.0, 0.0)
            ) { it !in DungeonUtils.dungeonTeammates.map { teammate -> teammate.entity } }

            mc.theWorld.setBlockToAir(chest)
            entitiesToRemove.forEach { it.setDead() }
            wrongPositions.remove(chest to entity)
        }
    }

    @SubscribeEvent
    fun onPacketSent(event: PacketEvent.Sent) {
        if (! inWeirdos) return
        if (trueTimeStart == null) return
        if (timeStart == null) return
        val packet = event.packet as? C08PacketPlayerBlockPlacement ?: return
        if (packet.position != correctPos) return

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
