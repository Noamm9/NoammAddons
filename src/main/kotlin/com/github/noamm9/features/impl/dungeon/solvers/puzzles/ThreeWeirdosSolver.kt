package com.github.noamm9.features.impl.dungeon.solvers.puzzles

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.PlayerInteractEvent
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.colorCorrect
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.colorWrong
import com.github.noamm9.features.impl.dungeon.solvers.puzzles.PuzzleSolvers.removeChests
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.map.core.RoomState
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils.rotate
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.floor

object ThreeWeirdosSolver {
    private val npcRegex = Regex("\\[NPC] (\\w+): (.+)")
    private var wrongPositions = CopyOnWriteArraySet<BlockPos>()
    private var correctPos: BlockPos? = null

    private val inThreeWeirdos get() = wrongPositions.isNotEmpty() || correctPos != null

    fun onChat(event: ChatMessageEvent) {
        if (! LocationUtils.inDungeon) return
        val currentRoom = ScanUtils.currentRoom.takeIf { it?.name == "Three Weirdos" } ?: return
        val (npcName, text) = npcRegex.find(event.unformattedText)?.destructured ?: return

        val isSolution = solutions.any { it.matches(text) }
        val isWrong = wrong.any { it.matches(text) }

        if (! isSolution && ! isWrong) return

        val correctNPC = mc.level?.entitiesForRendering()?.filterIsInstance<ArmorStand>()
            ?.find { it.name.unformattedText.contains(npcName) } ?: return

        val npcBlockPos = BlockPos(floor(correctNPC.x).toInt(), 69, floor(correctNPC.z).toInt())
        val chestPos = npcBlockPos.offset(BlockPos(1, 0, 0).rotate(360 - currentRoom.rotation !!))

        if (isSolution) {
            mc.player?.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 2f, 1f)
            correctPos = chestPos
        }
        else {
            if (removeChests.value) ThreadUtils.scheduledTask {
                WorldUtils.setBlockAt(chestPos, Blocks.AIR.defaultBlockState())
            }
            wrongPositions.add(chestPos)
        }
    }

    fun onRenderWorld(ctx: RenderContext) {
        if (! inThreeWeirdos) return

        correctPos?.let { pos ->
            Render3D.renderBlock(ctx, pos, colorCorrect.value)
        }

        if (! removeChests.value) wrongPositions.forEach { pos ->
            Render3D.renderBlock(ctx, pos, colorWrong.value)
        }
    }

    fun onInteract(event: PlayerInteractEvent.RIGHT_CLICK.BLOCK) {
        if (! inThreeWeirdos) return
        if (LocationUtils.inBoss) return
        if (event.pos == correctPos) reset()
    }

    fun onStateChange(event: DungeonEvent.RoomEvent.onStateChange) {
        if (! inThreeWeirdos) return
        if (event.room.name != "Three Weirdos") return
        if (event.newState != RoomState.DISCOVERED) return
        reset()
    }

    fun reset() {
        if (removeChests.value) {
            for (pos in wrongPositions) {
                WorldUtils.setBlockAt(pos, Blocks.CHEST.defaultBlockState())
            }
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