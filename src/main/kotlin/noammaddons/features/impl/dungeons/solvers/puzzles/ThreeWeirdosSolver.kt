package noammaddons.features.impl.dungeons.solvers.puzzles

import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.mc
import noammaddons.events.*
import noammaddons.features.impl.dungeons.dmap.handlers.DungeonInfo
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.Wcolor
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.WcolorWrong
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers.WremoveChests
import noammaddons.utils.BlockUtils.ghostBlock
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.ScanUtils.rotate
import java.util.concurrent.CopyOnWriteArraySet


object ThreeWeirdosSolver {
    private val npcRegex = Regex("\\[NPC] (\\w+): (.+)")
    private var wrongPositions = CopyOnWriteArraySet<BlockPos>()
    private var correctPos: BlockPos? = null


    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! PuzzleSolvers.weirdos.value) return
        if (! inDungeon || inBoss) return

        val (npc, msg) = npcRegex.find(event.component.noFormatText)?.destructured ?: return
        if (solutions.none { it.matches(msg) } && wrong.none { it.matches(msg) }) return

        val rot = DungeonInfo.uniqueRooms["Three Weirdos"]?.rotation ?: return
        val correctNPC = mc.theWorld?.loadedEntityList?.filterIsInstance<EntityArmorStand>()?.find { it.name.removeFormatting() == npc } ?: return
        val pos = BlockPos(correctNPC.posX - 0.5, 69.0, correctNPC.posZ - 0.5).add(BlockPos(1, 0, 0).rotate(360 - rot))

        if (solutions.any { it.matches(msg) }) {
            mc.thePlayer.playSound("note.pling", 2f, 1f)
            correctPos = pos
        }
        else {
            if (WremoveChests.value) mc.theWorld.setBlockToAir(pos)
            wrongPositions.add(pos)
        }
    }

    @SubscribeEvent
    fun onPazzleEvent(event: DungeonEvent.PuzzleEvent.Reset) {
        if (event.pazzle.room?.name != "Three Weirdos") return
        reset()
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        correctPos?.let { chest ->
            drawBlockBox(
                chest, Wcolor.value, phase = false,
                fill = true, outline = true
            )
        }
        if (! WremoveChests.value) wrongPositions.forEach { pos ->
            drawBlockBox(
                pos, WcolorWrong.value, phase = false,
                fill = true, outline = true
            )
        }
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Sent) {
        if (! PuzzleSolvers.weirdos.value) return
        if (! inDungeon || inBoss) return
        if (wrongPositions.size != 2) return
        val packet = event.packet as? C08PacketPlayerBlockPlacement ?: return
        if (packet.position != correctPos) return

        reset()
    }

    fun reset() {
        if (WremoveChests.value) wrongPositions.forEach {
            ghostBlock(it, Blocks.chest.defaultState)
        }

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