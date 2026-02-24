package com.github.noamm9.features.impl.F7

import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.SliderSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.network.PacketUtils.send
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.Vec3

object RelicAura: Feature("Picks up the Relic for you") {
    private val range by SliderSetting("Range", 3f, 1f, 5f, 0.2f)
    private var lastClick = System.currentTimeMillis()

    override fun init() {
        register<TickEvent.Start> {
            if(LocationUtils.F7Phase != 5) return@register
            if(System.currentTimeMillis() - lastClick < 200) return@register
            val armorStand = mc.level?.entitiesForRendering()?.filterIsInstance<ArmorStand>()?.firstOrNull {
                val headItem = it.getItemBySlot(EquipmentSlot.HEAD)
                headItem.displayName.string.contains("Relic") && distanceTo(it) < range.value
            } ?: return@register
            val hit = mc.hitResult!!.location
            ServerboundInteractPacket.createInteractionPacket(
                armorStand,
                mc.player!!.isShiftKeyDown,
                InteractionHand.MAIN_HAND,
                Vec3(hit.x - armorStand.x, hit.y - armorStand.y, hit.z - armorStand.z)
            ).send()
            lastClick = System.currentTimeMillis()
        }
    }

    private fun distanceTo(entity: Entity): Double {
        val position = mc.player!!.position().add(y = mc.player!!.eyeHeight)
        val entitypos = entity.position().add(y = entity.eyeHeight)
        return position.distanceTo(entitypos)
    }
}