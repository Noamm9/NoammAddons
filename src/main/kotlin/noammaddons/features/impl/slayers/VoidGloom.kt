package noammaddons.features.impl.slayers

import net.minecraft.entity.Entity

object VoidGloom {
    private fun getLaserTimer(entity: Entity): String {
        val ridingEntity = entity.ridingEntity ?: return ""
        val time = maxOf(0.0, 8.2 - (ridingEntity.ticksExisted / 20.0))
        val color = when {
            time > 6.0 -> "§a"
            time > 3.0 -> "§e"
            time > 1.0 -> "§6"
            else -> "§c"
        }
        return "$color${"%.1f".format(time)}s"
    }
}