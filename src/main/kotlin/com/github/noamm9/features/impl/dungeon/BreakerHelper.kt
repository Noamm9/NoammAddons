package com.github.noamm9.features.impl.dungeon

//#if CHEAT

import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.PlayerInteractEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.DungeonUtils.REDSTONE_KEY
import com.github.noamm9.utils.dungeons.map.core.RoomType
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SkullBlock
import net.minecraft.world.level.block.entity.SkullBlockEntity

object BreakerHelper: Feature("Utilities for Dungeon Breaker") {
    private val preventMine by ToggleSetting("Prevent Mine").jsonName("Prevent Secret Mine").withDescription("Prevents you from breaking blocks that cant be broken.")
    private val zeroPing by ToggleSetting("Zero Ping").withDescription("Removes the blocks you mine instantly instead of waiting for the server to remove them.")
    private val preventMiningWithoutCharges by ToggleSetting("Prevent Mining Without Charges").withDescription("Prevents you from breaking blocks when your breaker is out of charges")
    private val blacklist = setOf(
        Blocks.BARRIER, Blocks.BEDROCK, Blocks.COMMAND_BLOCK, Blocks.TNT, Blocks.CHEST, Blocks.PLAYER_HEAD,
        Blocks.PLAYER_WALL_HEAD, Blocks.TRAPPED_CHEST, Blocks.END_PORTAL_FRAME, Blocks.END_PORTAL, Blocks.STICKY_PISTON,
        Blocks.PISTON_HEAD, Blocks.PISTON, Blocks.MOVING_PISTON, Blocks.LEVER, Blocks.STONE_BUTTON,
        Blocks.SKELETON_SKULL, Blocks.SKELETON_WALL_SKULL, Blocks.WITHER_SKELETON_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL,
        Blocks.DISPENSER, Blocks.HOPPER, Blocks.DROPPER
    )

    // tooltip says +2 every sec but the server actually gives +2 three times every sec. thx hypixel
    private const val regenTime = 1000L / 3
    private const val regenAmount = 2
    private const val maxCharges = 20

    private var charges = maxCharges
    private var lastRegen = System.currentTimeMillis()

    override fun init() {
        register<PlayerInteractEvent.LEFT_CLICK.BLOCK> {
            if (! preventMine.value) return@register
            if (! LocationUtils.inDungeon) return@register
            if (event.item?.skyblockId != "DUNGEONBREAKER") return@register
            val block = WorldUtils.getBlockAt(event.pos)
            if (block !in blacklist) return@register
            if (LocationUtils.inBoss && block.equalsOneOf(Blocks.LEVER, Blocks.STONE_BUTTON)) return@register
            if (block is SkullBlock && (level.getBlockEntity(event.pos) as? SkullBlockEntity)?.ownerProfile?.partialProfile()?.id.toString().equalsOneOf(*REDSTONE_KEY)) return@register
            event.isCanceled = true
        }

        register<PlayerInteractEvent.LEFT_CLICK.BLOCK> {
            if (! preventMiningWithoutCharges.value) return@register
            if (! LocationUtils.inDungeon) return@register
            if (event.item?.skyblockId != "DUNGEONBREAKER") return@register
            if (WorldUtils.getBlockAt(event.pos) in blacklist) return@register
            if (getCharges() > 0) return@register
            event.isCanceled = true
        }
    }

    @JvmStatic
    fun onHitBlock(pos: BlockPos) {
        if (! enabled) return
        syncCharge(pos)

        if (! zeroPing.value) return
        if (! LocationUtils.inDungeon) return
        if (player.mainHandItem.skyblockId != "DUNGEONBREAKER") return
        if (ScanUtils.currentRoom?.data?.type.equalsOneOf(RoomType.PUZZLE, RoomType.FAIRY)) return
        val state = WorldUtils.getStateAt(pos).takeUnless { it.block in blacklist || it.block == Blocks.OBSIDIAN } ?: return

        level.removeBlock(pos, false)
        level.playLocalSound(
            pos.x + 0.5,
            pos.y + 0.5,
            pos.z + 0.5,
            state.soundType.breakSound,
            SoundSource.BLOCKS,
            (state.soundType.volume + 1.0f) / 2.0f,
            state.soundType.pitch * 0.8f,
            false
        )
    }

    private fun getCharges(): Int {
        val now = System.currentTimeMillis()
        val ticks = (now - lastRegen) / regenTime
        charges = (charges + ticks.toInt() * regenAmount).coerceAtMost(maxCharges)
        lastRegen = if (charges == maxCharges) now else lastRegen + ticks * regenTime
        return charges
    }

    private fun syncCharge(pos: BlockPos) {
        if (! LocationUtils.inDungeon) return
        if (player.mainHandItem.skyblockId != "DUNGEONBREAKER") return
        val block = WorldUtils.getBlockAt(pos)
        if (block in blacklist || block.equalsOneOf(Blocks.OBSIDIAN, Blocks.AIR)) return
        if (getCharges() <= 0) return
        charges --
    }
}
//#endif