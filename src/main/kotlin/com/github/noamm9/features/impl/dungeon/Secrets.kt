package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.config.types.*
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.init.ModCompatibility
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.Utils.send
import com.github.noamm9.utils.dungeons.enums.SecretType
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.width
import com.github.noamm9.utils.render.world.Render3D.renderBlock
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.concurrent.*

object Secrets: Feature() {
    private val hudDisplay by ToggleSetting("Secret HUD", true).withDescription("Displays the current room's secrets on screen.").section("HUD")

    //#if CHEAT
    private val closeChest by ToggleSetting("Close Chest").section("Auto").withDescription("Automatically closes the secret chest for you.")
    private val lever by ToggleSetting("Lever").withDescription("Expand block Lever hitbox.").section("Secret Hitboxes")
    private val leverSize by SliderSetting("Lever Hitbox Size", 1.0, 8.0 / 16.0, 1.0, 0.01).showIf { button.value }
    @JvmStatic val button by ToggleSetting("Button").withDescription("Expand button hitbox.")
    private val buttonSize by SliderSetting("Button Hitbox Size", 1.0, 6.0 / 16.0, 1.0, 0.01).showIf { button.value }
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
            return@hudElement line.width() to 9f
        }

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

        register<GameStartEvent> { if (lever.value || button.value) ModCompatibility.disableBlockstateCulling() }
        //#endif
    }

    //#if CHEAT
    override fun onEnable() {
        super.onEnable()
        if (lever.value || button.value) ModCompatibility.disableBlockstateCulling()
    }

    @JvmStatic
    fun getButtonShape(state: BlockState): VoxelShape {
        val face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE)
        val facing = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING)
        val powered = state.getValue(ButtonBlock.POWERED)
        val base = Block.boxZ(buttonSize.value * 16.0, 16.0 - if (powered) 1 else 2, 16.0)
        return Shapes.rotateAttachFace(base)[face]?.get(facing) !!
    }

    @JvmStatic
    fun getLeverShape(state: BlockState): VoxelShape {
        val face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE)
        val facing = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING)
        val base = Block.boxZ(leverSize.value * 16.0, leverSize.value * 16.0, 16.0 - leverSize.value * 16.0, 16.0)
        return Shapes.rotateAttachFace(base)[face]?.get(facing) !!
    }

    private val blackListedLevers = listOf(
        BlockPos(61, 136, 142), BlockPos(60, 136, 142),
        BlockPos(59, 136, 142), BlockPos(62, 135, 142),
        BlockPos(61, 135, 142), BlockPos(59, 135, 142),
        BlockPos(58, 135, 142), BlockPos(62, 134, 142),
        BlockPos(61, 134, 142), BlockPos(59, 134, 142),
        BlockPos(58, 134, 142), BlockPos(61, 133, 142),
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