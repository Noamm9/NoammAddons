package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.BlockChangeEvent
import com.github.noamm9.event.impl.CheckEntityGlowEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.KeybindSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.ui.clickgui.components.showIf
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.network.PacketUtils.send
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import org.lwjgl.glfw.GLFW
import java.awt.Color

object F4Features: Feature(name = "F4 Features", description = "Spirit bear spawn timer, highlights, and more.") {
    private val espThorn by ToggleSetting("ESP Thorn", true).section("Toggles")
    private val espSpiritBow by ToggleSetting("ESP Spirit Bow", true).withDescription("Highlights the Spirit Bow")
    private val espSpiritBear by ToggleSetting("ESP Spirit Bear", true).withDescription("Highlights the Spirit Bear")
    private val spiritBearHud by ToggleSetting("Spirit Bear HUD", true).withDescription("Shows the required mobs to spawn the Spirit Bear and a spawn timer for when he's about to spawn.")
    private val tribalSpearHold by ToggleSetting("AFK Thorn Stun", false).withDescription("Toggles right click for you while holding a Tribal Spear in the F4/M4 boss room.")
    private val tribalSpearKey by KeybindSetting("Spear Toggle Key", GLFW.GLFW_MOUSE_BUTTON_RIGHT).apply { isMouse = true }.showIf { tribalSpearHold.value }

    private val espThornColor by ColorSetting("Thorn Color", Color(255, 0, 0, 50)).section("Colors")
    private val espSpiritBearColor by ColorSetting("Bear Color", Color(255, 0, 255, 50))
    private val boxSpiritBowColor by ColorSetting("Box Color", Color(0, 255, 255, 50))

    private val inM4boss get() = LocationUtils.inBoss && LocationUtils.dungeonFloorNumber == 4
    private val blockLocations get() = if (LocationUtils.isMasterMode) m4BlockLocations else f4BlockLocations
    private val lastBlockLocation = BlockPos(7, 77, 34)
    private val spiritbows = HashSet<Int>()
    private const val bearSpawnTime = 68
    private var timer = - 1L
    private var count = 0
    private var spearToggled = false
    private var useItemSequence = 0


    override fun init() {
        hudElement("Spirit Bear", { spiritBearHud.value }, { inM4boss && ! DungeonListener.dungeonEnded }, centered = true) { ctx, example ->
            val time = timer - DungeonListener.currentTime
            val text = if (example) "4.25"
            else when {
                time > 0 -> (time / 20.0).toFixed(2)
                else -> "$count/${blockLocations.size}"
            }

            Render2D.drawCenteredString(ctx, text, 0f, 0f, Color(255, 0, 255))
            return@hudElement text.width().toFloat() to 9f
        }

        register<CheckEntityGlowEvent> {
            if (! inM4boss) return@register

            when {
                event.entity is Ghast -> if (espThorn.value) event.color = espThornColor.value
                event.entity is Player -> if (espSpiritBear.value && event.entity.gameProfile.name.lowercase().startsWith("spirit bear")) {
                    event.color = espSpiritBearColor.value
                }

                espSpiritBow.value && spiritbows.contains(event.entity.id) -> event.color = boxSpiritBowColor.value
            }
        }

        register<BlockChangeEvent> {
            if (! spiritBearHud.value || ! inM4boss) return@register
            if (event.pos !in blockLocations) return@register

            when (event.newBlock) {
                Blocks.SEA_LANTERN if event.oldBlock == Blocks.COAL_BLOCK -> {
                    if (event.pos == lastBlockLocation) timer = DungeonListener.currentTime + bearSpawnTime
                    count = (count + 1).coerceAtMost(blockLocations.size)
                }

                Blocks.COAL_BLOCK if event.oldBlock == Blocks.SEA_LANTERN -> {
                    if (event.pos == lastBlockLocation) timer = 0
                    count = (count - 1).coerceAtLeast(0)
                }
            }
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! espSpiritBow.value || ! inM4boss) return@register
            val packet = event.packet as? ClientboundSetEquipmentPacket ?: return@register
            val armorstand = mc.level?.getEntity(packet.entity) as? ArmorStand ?: return@register
            if (armorstand.getItemBySlot(EquipmentSlot.MAINHAND).hoverName.unformattedText != "Bow") return@register
            spiritbows.add(packet.entity)
        }

        register<TickEvent.Start> {
            val player = mc.player ?: return@register

            if (! tribalSpearHold.value || ! inM4boss || player.mainHandItem.skyblockId != "TRIBAL_SPEAR") {
                if (spearToggled) {
                    spearToggled = false
                    mc.options.keyUse.isDown = false
                }
                return@register
            }

            if (tribalSpearKey.isPressed()) {
                spearToggled = ! spearToggled
            }

            if (! spearToggled) {
                mc.options.keyUse.isDown = false
                return@register
            }

            if (mc.screen == null && mc.isWindowActive) {
                mc.options.keyUse.isDown = true
            } else {
                ServerboundUseItemPacket(InteractionHand.MAIN_HAND, useItemSequence ++, player.yRot, player.xRot).send()
            }
        }

        register<WorldChangeEvent> {
            spiritbows.clear()
            timer = - 1
            count = 0
            spearToggled = false
            useItemSequence = 0
        }
    }


    private val f4BlockLocations = hashSetOf(
        BlockPos(- 3, 77, 33), BlockPos(- 9, 77, 31), BlockPos(- 16, 77, 26), BlockPos(- 20, 77, 20), BlockPos(- 23, 77, 13),
        BlockPos(- 24, 77, 6), BlockPos(- 24, 77, 0), BlockPos(- 22, 77, - 7), BlockPos(- 18, 77, - 13), BlockPos(- 12, 77, - 19),
        BlockPos(- 5, 77, - 22), BlockPos(1, 77, - 24), BlockPos(8, 77, - 24), BlockPos(14, 77, - 23), BlockPos(21, 77, - 19),
        BlockPos(27, 77, - 14), BlockPos(31, 77, - 8), BlockPos(33, 77, - 1), BlockPos(34, 77, 5), BlockPos(33, 77, 12),
        BlockPos(31, 77, 19), BlockPos(27, 77, 25), BlockPos(20, 77, 30), BlockPos(14, 77, 33), BlockPos(7, 77, 34)
    )

    private val m4BlockLocations = hashSetOf(
        BlockPos(- 2, 77, 33), BlockPos(- 7, 77, 32), BlockPos(- 13, 77, 28), BlockPos(- 17, 77, 24), BlockPos(- 21, 77, 18),
        BlockPos(- 23, 77, 13), BlockPos(- 24, 77, 7), BlockPos(- 24, 77, 2), BlockPos(- 23, 77, - 4), BlockPos(- 21, 77, - 9),
        BlockPos(- 17, 77, - 14), BlockPos(- 12, 77, - 19), BlockPos(- 6, 77, - 22), BlockPos(- 1, 77, - 23), BlockPos(5, 77, - 24),
        BlockPos(10, 77, - 24), BlockPos(16, 77, - 22), BlockPos(21, 77, - 19), BlockPos(27, 77, - 15), BlockPos(30, 77, - 10),
        BlockPos(32, 77, - 5), BlockPos(34, 77, 1), BlockPos(34, 77, 7), BlockPos(33, 77, 12), BlockPos(31, 77, 18),
        BlockPos(28, 77, 23), BlockPos(23, 77, 28), BlockPos(18, 77, 31), BlockPos(12, 77, 33), BlockPos(7, 77, 34)
    )
}