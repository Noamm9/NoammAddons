package com.github.noamm9.utils.items

import com.github.noamm9.features.Shortcuts
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.PlayerUtils.isSneakingServer
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.items.ItemUtils.customData
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull

object TeleportUtils: Shortcuts {
    val INTERACTABLE_BLOCKS = listOf(
        Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.HOPPER,
        Blocks.CAULDRON, Blocks.LEVER, Blocks.STONE_BUTTON, Blocks.OAK_BUTTON,
        Blocks.OAK_TRAPDOOR, Blocks.IRON_TRAPDOOR
    )

    val TILLABLE_BLOCKS = setOf(
        Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT,
        Blocks.PODZOL, Blocks.MYCELIUM, Blocks.MOSS_BLOCK,
        Blocks.ROOTED_DIRT,
    )

    @Suppress("RedundantIf")
    fun canTeleport(yaw: Float, pitch: Float): Boolean {
        if (player.isPassenger) return false
        if (LocationUtils.dungeonFloorNumber == 7 && LocationUtils.inBoss) return false
        if (ActionBarParser.currentMana + ActionBarParser.overflowMana < ActionBarParser.maxMana * 0.1) return false
        if (ScanUtils.currentRoom?.data?.name.equalsOneOf("New Trap", "Old Trap", "Teleport Maze", "Boulder")) return false
        PlayerUtils.getSelectionBlock()?.let { if (WorldUtils.getBlockAt(it) in INTERACTABLE_BLOCKS) return false }
        if (isTargetingNPC(player, player.position().add(y = player.eyeHeight), MathUtils.getLookVec(yaw, pitch))) return false
        return true
    }

    fun getInfo(stack: ItemStack?): Info? {
        if (stack == null || stack.isEmpty) return null
        val sbId = stack.skyblockId
        val nbt = stack.customData

        if (sbId.equalsOneOf("ASPECT_OF_THE_VOID", "ASPECT_OF_THE_END")) {
            val tuners = nbt.getByte("tuned_transmission").getOrNull()?.toDouble() ?: .0

            return if (player.isSneakingServer && nbt.getByte("ethermerge").orElse(0) == 1.toByte()) {
                Info(57 + tuners, Etherwarp)
            }
            else Info(8 + tuners, InstantTransmission)
        }
        else if (nbt.getList("ability_scroll").toString().containsAll("SHADOW_WARP_SCROLL", "IMPLOSION_SCROLL", "WITHER_SHIELD_SCROLL")) {
            return Info(10.0, WitherImpact)
        }

        return null
    }

    private fun isTargetingNPC(player: Entity, startPos: Vec3, look: Vec3): Boolean {
        val maxDistance = 4.0
        val endPos = startPos.add(look.scale(maxDistance))

        val context = ClipContext(startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)
        val blockHitDistance = player.level().clip(context).location.distanceTo(startPos)

        val searchBox = player.boundingBox.expandTowards(look.scale(maxDistance)).inflate(1.0)
        val entityHit = ProjectileUtil.getEntityHitResult(
            player, startPos, endPos, searchBox,
            { entity -> ! entity.isSpectator && entity != player },
            Mth.square(blockHitDistance)
        ) ?: return false

        val target = entityHit.entity.takeUnless { it.customName?.unformattedText == "CLICK" } ?: return true

        val possibleEntities = player.level().getEntities(
            target, target.boundingBox.move(0.0, - 1.0, 0.0)
        ) { it is ArmorStand }

        return possibleEntities.any { it.customName?.unformattedText == "CLICK" }
    }

    const val Etherwarp = 0
    const val InstantTransmission = 1
    const val WitherImpact = 2

    data class Info(val distance: Double, val type: Int)
    data class Prediction(val position: Vec3, val info: Info)
}