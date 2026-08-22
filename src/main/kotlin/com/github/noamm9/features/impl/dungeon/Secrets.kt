package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.GameStartEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.init.ModCompatibility
import com.github.noamm9.utils.ActionBarParser
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.Utils.send
import com.github.noamm9.utils.dungeons.enums.SecretType
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.world.Render3D.renderBlock
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.concurrent.*

object Secrets: Feature() {
    private val hudDisplay by ToggleSetting("Secret HUD", true).withDescription("Displays the current room's secrets on screen.").section("HUD")

    //#if CHEAT
    private val closeChest by ToggleSetting("Close Chest").section("Auto").withDescription("Automatically closes the secret chest for you.")
    private val lever by ToggleSetting("Lever").withDescription("Full block Lever hitbox.").section("Secret Hitboxes")
    @JvmStatic val button by ToggleSetting("Button").withDescription("Full block button hitbox.")
    @JvmStatic val skull by ToggleSetting("Skulls").withDescription("Full block Skull hitbox.")
    @JvmStatic val mushroom by ToggleSetting("Mushroom").withDescription("Full block Mushroom hitbox.")
    //#endif

    private val secretClicked by ToggleSetting("Highlight Clicked Secret").withDescription("Highlights the block of a secret when you interact with it.").section("Secret Clicked")
    private val displayTime by SliderSetting("Highlight Time", 2.0, 0.5, 5.0, 0.1).withDescription("How long (in seconds) the highlight box remains visible.").showIf { secretClicked.value }
    private val secretClickedColor by ColorSetting("Highlight Color", Utils.favoriteColor.withAlpha(50)).withDescription("The color of the secret highlight box.").showIf { secretClicked.value }
    private val mode by DropdownSetting("Render Mode", 2, listOf("Fill", "Outline", "Filled Outline")).withDescription("Choose how the box is rendered.").showIf { secretClicked.value }
    private val phase by ToggleSetting("See Through Walls").withDescription("If enabled, the highlight will be visible through other blocks.").showIf { secretClicked.value }

    private val secretSound by ToggleSetting("Secret Sound").withDescription("Plays a sound effect when a secret is clicked/found.").section("Secret Sound")
    private val playSound = createSoundSettings("Sound", SoundEvents.EXPERIENCE_ORB_PICKUP) { secretSound.value }

    private val clicked = ConcurrentHashMap<BlockPos, Long>()
    private var lastPlayed = System.currentTimeMillis()

    override fun init() {
        hudElement("Secret Hud", { hudDisplay.value }, { LocationUtils.inDungeon && ! LocationUtils.inBoss }) { ctx, example ->
            val line = if (example) "&7Secrets: &c3&7/&a7"
            else {
                val max = ActionBarParser.maxSecrets ?: return@hudElement 0f to 0f
                val current = ActionBarParser.secrets ?: return@hudElement 0f to 0f
                "&7Secrets: ${ColorUtils.colorCodeByPercent(current, max)}$current&7/&a$max"
            }

            ctx.drawString(line, 0, 0)
            return@hudElement line.width().toFloat() to 9f
        }

        //#if CHEAT
        register<MainThreadPacketReceivedEvent.Pre> {
            if (! closeChest.value) return@register
            if (! LocationUtils.inDungeon) return@register
            if (LocationUtils.inBoss) return@register
            val packet = event.packet as? ClientboundOpenScreenPacket ?: return@register
            if (! packet.type.equalsOneOf(MenuType.GENERIC_9x3, MenuType.GENERIC_9x6)) return@register
            if (! packet.title.unformattedText.equalsOneOf("Chest", "Large Chest")) return@register
            ServerboundContainerClosePacket(packet.containerId).send()
            event.isCanceled = true
        }

        register<GameStartEvent> { ModCompatibility.disableBlockstateCulling() }
        //#endif

        register<RenderWorldEvent> {
            if (clicked.isEmpty()) return@register

            val outline = mode.value.equalsOneOf(1, 2)
            val fill = mode.value.equalsOneOf(0, 2)

            for ((pos, time) in clicked) {
                if (time + (displayTime.value * 1000) < System.currentTimeMillis()) {
                    clicked.remove(pos)
                    continue
                }

                event.ctx.renderBlock(pos, secretClickedColor.value, outline, fill, phase.value)
            }
        }

        register<DungeonEvent.SecretEvent> {
            if (secretSound.value) {
                if (event.type == SecretType.ITEM && System.currentTimeMillis() - lastPlayed < 2000) return@register
                if (event.type == SecretType.CHEST) lastPlayed = System.currentTimeMillis()
                if (clicked.containsKey(event.pos)) return@register
                playSound.action.invoke()
            }

            if (secretClicked.value) {
                if (clicked.containsKey(event.pos)) return@register
                clicked[event.pos] = System.currentTimeMillis()
            }
        }
    }

    //#if CHEAT
    override fun onEnable() {
        super.onEnable()
        ModCompatibility.disableBlockstateCulling()
    }

    @JvmStatic
    fun getButtonShape(state: BlockState): VoxelShape {
        val face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE)
        val direction = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING)
        val powered = state.getValue(ButtonBlock.POWERED)

        val f2 = (if (powered) 1 else 2) / 16.0
        return when (face) {
            AttachFace.CEILING -> Shapes.box(0.0, 1.0 - f2, 0.0, 1.0, 1.0, 1.0)
            AttachFace.FLOOR -> Shapes.box(0.0, 0.0, 0.0, 1.0, 0.0 + f2, 1.0)
            else -> when (direction) {
                Direction.EAST -> Shapes.box(0.0, 0.0, 0.0, f2, 1.0, 1.0)
                Direction.WEST -> Shapes.box(1.0 - f2, 0.0, 0.0, 1.0, 1.0, 1.0)
                Direction.SOUTH -> Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, f2)
                Direction.NORTH -> Shapes.box(0.0, 0.0, 1.0 - f2, 1.0, 1.0, 1.0)
                Direction.UP -> Shapes.box(0.0, 0.0, 0.0, 1.0, 0.0 + f2, 1.0)
                Direction.DOWN -> Shapes.box(0.0, 1.0 - f2, 0.0, 1.0, 1.0, 1.0)
            }
        }
    }

    private val blackListedLevers = listOf(
        BlockPos(61, 136, 142), BlockPos(60, 136, 142), BlockPos(59, 136, 142),
        BlockPos(62, 135, 142), BlockPos(61, 135, 142), BlockPos(59, 135, 142),
        BlockPos(58, 135, 142), BlockPos(62, 134, 142), BlockPos(61, 134, 142),
        BlockPos(59, 134, 142), BlockPos(58, 134, 142), BlockPos(61, 133, 142),
        BlockPos(60, 133, 142), BlockPos(59, 133, 142)
    )

    @JvmStatic
    fun isValidLever(pos: BlockPos): Boolean {
        if (! enabled) return false
        if (! lever.value) return false
        if (pos in blackListedLevers && LocationUtils.dungeonFloorNumber == 7) return false
        return true
    }
    //#endif
}