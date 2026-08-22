package com.github.noamm9.features.impl.dungeon.solvers.puzzles

import com.github.noamm9.event.impl.*
import com.github.noamm9.utils.render.world.RenderContext

sealed interface PuzzleSolver {
    val enabled: Boolean

    fun reset() {}
    fun onRoomExit() = reset()
    fun onStateChange(event: DungeonEvent.RoomEvent.onStateChange) {}
    fun onRoomEnter(event: DungeonEvent.RoomEvent.onEnter) {}
    fun onTick() {}
    fun onPacket(event: MainThreadPacketReceivedEvent.Pre) {}
    fun onChat(event: ChatMessageEvent) {}
    fun onEntityGlow(event: CheckEntityGlowEvent) {}
    fun onRenderWorld(ctx: RenderContext) {}
    fun onInteract(event: PlayerInteractEvent.RIGHT_CLICK.BLOCK) {}
}