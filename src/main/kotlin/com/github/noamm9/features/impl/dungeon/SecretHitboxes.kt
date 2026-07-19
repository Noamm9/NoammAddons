@file:Suppress("UNNECESSARY_SAFE_CALL")
package com.github.noamm9.features.impl.dungeon

//#if CHEAT

import com.github.noamm9.event.impl.GameStartEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.utils.location.LocationUtils
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

object SecretHitboxes: Feature("Changes the hitboxes of secret blocks to be larger.") {
    val lever by ToggleSetting("Lever").withDescription("Full block Lever hitbox.")
        .onChange { mc.levelRenderer?.allChanged() }

    @JvmStatic
    val leverWidth by SliderSetting("Lever Width", 1.0f, 0.0f, 1.0f, 0.05f)
        .showIf { lever.value }
        .withDescription("Lever hitbox width (X axis). 0.0 means vanilla size.")
        .onChange { mc.levelRenderer?.allChanged() }

    @JvmStatic
    val leverHeight by SliderSetting("Lever Height", 1.0f, 0.0f, 1.0f, 0.05f)
        .showIf { lever.value }
        .withDescription("Lever hitbox height (Y axis). 0.0 means vanilla size.")
        .onChange { mc.levelRenderer?.allChanged() }

    @JvmStatic
    val leverLength by SliderSetting("Lever Length", 1.0f, 0.0f, 1.0f, 0.05f)
        .showIf { lever.value }
        .withDescription("Lever hitbox length (Z axis). 0.0 means vanilla size.")
        .onChange { mc.levelRenderer?.allChanged() }

    @JvmStatic
    val button by ToggleSetting("Button").withDescription("Full block button hitbox.")
        .onChange { mc.levelRenderer?.allChanged() }

    @JvmStatic
    val buttonSize by SliderSetting("Button Size", 1.0f, 0.0f, 1.0f, 0.05f)
        .showIf { button.value }
        .withDescription("Button hitbox size ratio. 0.0 means vanilla size.")
        .onChange { mc.levelRenderer?.allChanged() }

    @JvmStatic
    val skull by ToggleSetting("Skulls").withDescription("Full block Skull hitbox.")
        .onChange { mc.levelRenderer?.allChanged() }

    @JvmStatic
    val mushroom by ToggleSetting("Mushroom").withDescription("Full block Mushroom hitbox.")
        .onChange { mc.levelRenderer?.allChanged() }

    override fun init() {
        register<GameStartEvent> { disableBlockstateCulling() }
    }

    override fun onEnable() {
        super.onEnable()
        disableBlockstateCulling()
        mc.levelRenderer?.allChanged()
    }

    override fun onDisable() {
        super.onDisable()
        mc.levelRenderer?.allChanged()
    }

    private fun disableBlockstateCulling() {
        if (! FabricLoader.getInstance().isModLoaded("moreculling")) return
        val main = Class.forName("ca.fxco.moreculling.MoreCulling")
        val config = main.getDeclaredField("CONFIG").get(null)

        val blockStateCulling = config?.javaClass?.getDeclaredField("useBlockStateCulling")
        blockStateCulling?.isAccessible = true
        blockStateCulling?.setBoolean(config, false)
    }

    @JvmStatic
    fun getButtonShape(state: BlockState, size: Float): VoxelShape {
        val face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE)
        val direction = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING)
        val powered = state.getValue(ButtonBlock.POWERED)

        val f2 = (if (powered) 1 else 2) / 16.0
        val baseSize = 0.375
        val lateralSize = baseSize + size.toDouble() * (1.0 - baseSize)
        val low = 0.5 - (lateralSize / 2.0)
        val high = 0.5 + (lateralSize / 2.0)

        return when (face) {
            AttachFace.CEILING -> Shapes.box(low, 1.0 - f2, low, high, 1.0, high)
            AttachFace.FLOOR -> Shapes.box(low, 0.0, low, high, 0.0 + f2, high)
            else -> when (direction) {
                Direction.EAST -> Shapes.box(0.0, low, low, f2, high, high)
                Direction.WEST -> Shapes.box(1.0 - f2, low, low, 1.0, high, high)
                Direction.SOUTH -> Shapes.box(low, low, 0.0, high, high, f2)
                Direction.NORTH -> Shapes.box(low, low, 1.0 - f2, high, high, 1.0)
                Direction.UP -> Shapes.box(low, 0.0, low, high, 0.0 + f2, high)
                Direction.DOWN -> Shapes.box(low, 1.0 - f2, low, high, 1.0, high)
            }
        }
    }

    @JvmStatic
    fun getLeverShape(state: BlockState, w: Float, h: Float, l: Float): VoxelShape {
        val face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE)
        val direction = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING)

        val baseHeight = 0.375
        val baseWidthLength = 0.44

        val effW = baseWidthLength + w.toDouble() * (1.0 - baseWidthLength)
        val effH = baseHeight + h.toDouble() * (1.0 - baseHeight)
        val effL = baseWidthLength + l.toDouble() * (1.0 - baseWidthLength)

        val lowW = 0.5 - (effW / 2.0)
        val highW = 0.5 + (effW / 2.0)
        val lowL = 0.5 - (effL / 2.0)
        val highL = 0.5 + (effL / 2.0)

        return when (face) {
            AttachFace.CEILING -> Shapes.box(lowW, 1.0 - effH, lowL, highW, 1.0, highL)
            AttachFace.FLOOR -> Shapes.box(lowW, 0.0, lowL, highW, effH, highL)
            else -> when (direction) {
                Direction.EAST -> Shapes.box(0.0, lowW, lowL, effH, highW, highL)
                Direction.WEST -> Shapes.box(1.0 - effH, lowW, lowL, 1.0, highW, highL)
                Direction.SOUTH -> Shapes.box(lowL, lowW, 0.0, highL, highW, effH)
                Direction.NORTH -> Shapes.box(lowL, lowW, 1.0 - effH, highL, highW, 1.0)
                else -> Shapes.box(lowW, 0.0, lowL, highW, effH, highL)
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
}
//#endif