package noammaddons.features.dungeons.ESP

import net.minecraft.entity.Entity
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PostRenderEntityModelEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.EspUtils.EspMob
import noammaddons.utils.RenderUtils.drawEntityBox
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color

object WitherESP: Feature() {
    private data class Wither(var entity: Entity?, val color: Color)

    private val WitherType = mapOf(
        "MAXOR" to Wither(null, Color(88, 4, 164)),
        "STORM" to Wither(null, Color(0, 208, 255)),
        "GOLDOR" to Wither(null, Color(110, 110, 110)),
        "NECRON" to Wither(null, Color(255, 0, 0)),
        "VANQUISHER" to Wither(null, Color(88, 4, 164))
    )

    private lateinit var type: Wither

    @SubscribeEvent
    fun esp(event: PostRenderEntityModelEvent) {
        if (! config.espWithers) return
        if (event.entity is EntityArmorStand) {
            val entity = event.entity
            val name = entity.displayName.unformattedText.lowercase().removeFormatting()

            type = when {
                name.contains("maxor") -> WitherType["MAXOR"] !!
                name.contains("goldor") -> WitherType["GOLDOR"] !!
                name.contains("storm") -> WitherType["STORM"] !!
                name.contains("necron") -> WitherType["NECRON"] !!
                name.contains("vanquisher") -> WitherType["VANQUISHER"] !!
                else -> return
            }

            entity.entityWorld.getEntitiesInAABBexcluding(
                entity, entity.entityBoundingBox.offset(0.0, - 1.0, 0.0)
            ) { ! (it?.isInvisible ?: true) }?.run {
                type.entity = firstOrNull { it is EntityWither } ?: return@run
            }
        }

        if (! config.espType.equalsOneOf(0, 2)) return
        if (WitherType.values.none { it.entity == event.entity }) return

        EspMob(event, type.color)
    }

    @SubscribeEvent
    fun renderBoxs(event: RenderWorld) {
        if (! config.espWithers) return
        if (config.espType != 1) return
        mc.theWorld?.loadedEntityList?.forEach { entity ->
            if (WitherType.values.none { entity == it.entity }) return@forEach

            drawEntityBox(entity, type.color)
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        WitherType.values.forEach { it.entity = null }
    }
}