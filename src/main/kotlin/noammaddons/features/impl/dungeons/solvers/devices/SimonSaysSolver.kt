package noammaddons.features.impl.dungeons.solvers.devices

import gg.essential.elementa.utils.withAlpha
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.LocationUtils.P3Section
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.distance2D
import noammaddons.utils.RenderHelper
import noammaddons.utils.RenderUtils
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color


object SimonSaysSolver: Feature() {
    private val blockWrongClicks by ToggleSetting("Block Wrong Clicks")

    private val blocks = mutableSetOf<BlockPos>()
    private val startObsidianBlock = BlockPos(111, 120, 92)
    private val devStartBtn = BlockPos(110, 121, 91)
    private var lastExisted = false

    private val atSS get() = enabled && distance2D(startObsidianBlock, mc.thePlayer?.position ?: BlockPos(0, 0, 0)) < 15 && P3Section == 1

    private fun getColor(index: Int) = when (index) {
        0 -> Color.GREEN
        1 -> Color.YELLOW
        else -> Color.RED
    }.withAlpha(100)

    @SubscribeEvent
    fun onTick(event: Tick) {
        if (! atSS) return blocks.clear()
        val buttonsExist = getBlockAt(startObsidianBlock.add(- 1, 0, 0)) == Blocks.stone_button
        if (buttonsExist && ! lastExisted) lastExisted = true

        if (! buttonsExist && lastExisted) {
            lastExisted = false
            blocks.clear()
        }

        for (dy in 0 .. 3) {
            for (dz in 0 .. 3) {
                val pos = startObsidianBlock.add(0, dy, dz)
                if (getBlockAt(pos) != Blocks.sea_lantern) continue
                blocks.add(pos)
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(e: RenderWorld) {
        if (blocks.isEmpty()) return

        RenderUtils.drawString(
            "${RenderHelper.colorCodeByPresent(blocks.size, 5)}${blocks.size}",
            Vec3(devStartBtn).add(0.7, 1, 0.5), scale = 2f, phase = false
        )

        blocks.withIndex().forEach { (index, pos) ->
            val color = getColor(index)
            RenderUtils.drawBox(
                pos.x - 0.13,
                pos.y + 0.37,
                pos.z + 0.3,
                color, outline = true,
                fill = true, phase = false,
                width = 0.4,
                height = 0.26
            )
        }
    }

    @SubscribeEvent
    fun onMouse(event: MouseEvent) {
        if (! blockWrongClicks) return
        if (! atSS) return
        if (mc.thePlayer.isSneaking) return
        if (! event.buttonstate) return
        if (! event.button.equalsOneOf(0, 1)) return
        val lookPos = mc?.objectMouseOver?.blockPos ?: return
        if (lookPos == devStartBtn) return
        val blocksArr = blocks.toTypedArray()
        if (blocksArr.isEmpty()) return
        val obsidianPos = lookPos.add(1, 0, 0)
        if (getBlockAt(lookPos) != Blocks.stone_button) return

        if (obsidianPos == blocksArr[0]) return
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onPacketSent(event: PacketEvent.Sent) {
        if (! atSS) return

        when (event.packet) {
            is C08PacketPlayerBlockPlacement -> {
                val blocksArr = blocks.toTypedArray()
                if (event.packet.position == devStartBtn) return blocks.clear()
                if (getBlockAt(event.packet.position) != Blocks.stone_button) return
                if (blocksArr.isEmpty()) return
                if (event.packet.position.add(1, 0, 0) != blocksArr[0] && ! mc.thePlayer.isSneaking && blockWrongClicks) return event.setCanceled(true)
                blocks.remove(event.packet.position.add(1, 0, 0))
            }

            is C07PacketPlayerDigging -> {
                val blocksArr = blocks.toTypedArray()
                if (event.packet.position == devStartBtn) return blocks.clear()
                if (getBlockAt(event.packet.position) != Blocks.stone_button) return
                if (blocksArr.isEmpty()) return
                if (event.packet.position.add(1, 0, 0) != blocksArr[0] && ! mc.thePlayer.isSneaking && blockWrongClicks) return event.setCanceled(true)
                blocks.remove(event.packet.position.add(1, 0, 0))
            }
        }
    }
}