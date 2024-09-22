package NoammAddons.features.General

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.utils.ItemUtils
import NoammAddons.utils.RenderUtils.drawString
import NoammAddons.utils.RenderUtils.drawEntityBox
import NoammAddons.utils.RenderUtils.getRenderX
import NoammAddons.utils.RenderUtils.getRenderY
import NoammAddons.utils.RenderUtils.getRenderZ
import net.minecraft.entity.item.EntityItem
import net.minecraft.util.Vec3
import java.awt.Color


object CustomItemEntity {
    fun customItemEntity(entity: EntityItem): Boolean {
        if (!config.CustomItemEntity) return false
        val color = ItemUtils.getRarity(entity.entityItem).color

        drawEntityBox(
            entity,
            Color(color.red, color.green, color.blue, 85),
            true, true
        )

        drawString(
            entity.entityItem.displayName,
            Vec3(entity.getRenderX(), entity.getRenderY() + 1.3, entity.getRenderZ()),
            Color(255, 255, 255, 255),
            1f
        )
        return true
    }
}
