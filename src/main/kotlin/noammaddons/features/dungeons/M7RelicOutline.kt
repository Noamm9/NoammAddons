package noammaddons.features.dungeons

import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.BlockUtils.toBlockPos
import noammaddons.utils.BlockUtils.toVec3
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.RenderUtils
import noammaddons.utils.RenderUtils.drawBox
import java.awt.Color

object M7RelicOutline {
    private data class RelicCauldron(val cauldronPos: Vec3, val color: Color, val relicPos: BlockPos)
    private val RelicCauldrons = mapOf(
        "Corrupted Blue Relic" to RelicCauldron(Vec3(59.0, 7.0, 44.0), Color(0, 138, 255, 110), BlockPos(91, 7, 94)),
        "Corrupted Orange Relic" to RelicCauldron(Vec3(57.0, 7.0, 42.0), Color(255, 114, 0, 110), BlockPos(92, 7, 56)),
        "Corrupted Purple Relic" to RelicCauldron(Vec3(54.0, 7.0, 41.0), Color(129, 0, 111, 110), BlockPos(56, 9, 132)),
        "Corrupted Red Relic" to RelicCauldron(Vec3(51.0, 7.0, 42.0), Color(255, 0, 0, 110), BlockPos(20, 7, 59)),
        "Corrupted Green Relic" to RelicCauldron(Vec3(49.0, 7.0, 44.0), Color(0, 255, 0, 110), BlockPos(20, 7, 94))
    )
    private var drawOutline = true

    @SubscribeEvent
    fun RelicOutline(event: RenderWorldLastEvent) {
        if (!config.M7RelicOutline) return
        if (F7Phase != 5) return

        RelicCauldrons[Player?.inventory?.mainInventory?.get(8)?.displayName.removeFormatting()]?.run {
            RenderUtils.drawBlockBox(cauldronPos.toBlockPos(), color, outline = true, fill = true, phase = true)
            RenderUtils.drawTracer(cauldronPos.add(Vec3(0.5, 0.5, 0.5)), color)
        }

        if (!drawOutline) return

        RelicCauldrons.forEach {
            it.value.run {
                drawBox(
                    relicPos.x+0.25,
                    relicPos.y+0.3,
                    relicPos.z+0.25,
                    color, true, true,
                    width = 0.5,
                    height = 0.5
                )
            }
        }
    }

    @SubscribeEvent
    fun onChat(e: Chat) {
        if (!e.component.unformattedText.equals("[BOSS] Wither King: You... again?")) return

        drawOutline = false
    }

    @SubscribeEvent
    fun onWorldUnload(e: WorldEvent.Unload) {
        if (!config.M7RelicOutline) return
        if (drawOutline) return

        drawOutline = true
    }
}