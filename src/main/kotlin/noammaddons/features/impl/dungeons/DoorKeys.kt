package noammaddons.features.impl.dungeons

import gg.essential.elementa.utils.withAlpha
import net.minecraft.entity.Entity
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.MathUtils.add
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderHelper.renderX
import noammaddons.utils.RenderHelper.renderY
import noammaddons.utils.RenderHelper.renderZ
import noammaddons.utils.RenderUtils.drawBox
import noammaddons.utils.RenderUtils.drawTracer
import java.awt.Color

object DoorKeys: Feature("Highlight blood/wither door key in the dungeon") {

    private val highlightWither = ToggleSetting("Wither Key")
    private val highlightBlood = ToggleSetting("Blood Key")
    private val witherColor = ColorSetting("Wither Key Color", Color.BLACK.withAlpha(60)).addDependency(highlightWither)
    private val bloodColor = ColorSetting("Blood Key Color", Color.RED.withAlpha(60)).addDependency(highlightBlood)
    override fun init() = addSettings(highlightWither, highlightBlood, SeperatorSetting("Colors"), witherColor, bloodColor)

    private var doorKey: Pair<Entity, Color>? = null

    @SubscribeEvent
    fun onPacket(event: PostEntityMetadataEvent) {
        if (inBoss) return
        when (event.entity.name.removeFormatting()) {
            "Wither Key" -> doorKey = Pair(event.entity, witherColor.value)
            "Blood Key" -> doorKey = Pair(event.entity, bloodColor.value)
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (inBoss) return

        doorKey?.let { (entity, color) ->
            if (entity.isDead) {
                doorKey = null
                return
            }

            drawTracer(entity.renderVec.add(y = 1.7), color)
            drawBox(
                entity.renderX - 0.4,
                entity.renderY + 1.2f,
                entity.renderZ - 0.4,
                color,
                outline = true,
                fill = true,
                width = 0.8,
                height = 0.8,
                phase = true,
            )
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        doorKey = null
    }
}
