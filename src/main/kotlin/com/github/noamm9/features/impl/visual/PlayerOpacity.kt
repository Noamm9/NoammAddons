package com.github.noamm9.features.impl.visual

import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.CheckEntityRenderEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.entity.Entity

/**
 * @see com.github.noamm9.mixin.MixinEntityRenderer
 * @see com.github.noamm9.mixin.MixinEntityRenderDispatcher
 * @see com.github.noamm9.mixin.MixinLivingEntityRenderer
 * @see com.github.noamm9.mixin.MixinEquipmentLayerRenderer
 */
object PlayerOpacity: Feature("Makes nearby players transparent so they don't block your view.") {
    private val dungeonOnly by ToggleSetting("Dungeons only").withDescription("Only enables this feature inside dungeons.")
    private val distance by SliderSetting("Distance", 3, 1, 32, 1).withDescription("How close a player has to be to become transparent.")
    private val opacity by SliderSetting("Opacity", 30, 0, 100, 1, "%").withDescription("0% hides them completely.")

    override fun init() {
        register<CheckEntityRenderEvent> {
            if (opacity.value != 0) return@register
            if (! isNearby(event.entity)) return@register
            event.isCanceled = true
        }
    }

    @JvmStatic
    fun getOpacity(entity: Entity): Float {
        if (! enabled) return 1f
        if (! isNearby(entity)) return 1f
        return opacity.value / 100f
    }

    private fun isNearby(entity: Entity): Boolean {
        if (dungeonOnly.value && ! LocationUtils.inDungeon) return false
        if (entity !is AbstractClientPlayer) return false
        if (entity == player) return false
        if (entity.uuid.version() != 4) return false
        if (entity.distanceToSqr(player) > distance.value * distance.value) return false
        return true
    }
}