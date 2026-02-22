package com.github.noamm9.features.impl.F7.dragons

import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.Blessing
import com.github.noamm9.utils.dungeons.enums.DungeonClass

object DragonPriority {
    fun findPriority(spawningDragons: MutableList<WitherDragonEnum>): WitherDragonEnum {
        return if (! WitherDragons.dragonPriorityToggle.value) {
            spawningDragons.sortBy { listOf(WitherDragonEnum.Red, WitherDragonEnum.Orange, WitherDragonEnum.Blue, WitherDragonEnum.Purple, WitherDragonEnum.Green).indexOf(it) }
            spawningDragons[0]
        }
        else sortPriority(spawningDragons)
    }

    private fun sortPriority(spawningDragons: MutableList<WitherDragonEnum>): WitherDragonEnum {
        val totalPower = Blessing.POWER.current + (if (Blessing.TIME.current > 0) 2.5 else 0.0)
        val playerClass = DungeonListener.thePlayer?.clazz

        val dragonList = listOf(WitherDragonEnum.Orange, WitherDragonEnum.Green, WitherDragonEnum.Red, WitherDragonEnum.Blue, WitherDragonEnum.Purple)
        val priorityList =
            if (totalPower >= WitherDragons.normalPower.value || (spawningDragons.any { it == WitherDragonEnum.Purple } && totalPower >= WitherDragons.easyPower.value))
                if (playerClass.equalsOneOf(DungeonClass.Berserk, DungeonClass.Mage)) dragonList else dragonList.reversed()
            else listOf(WitherDragonEnum.Red, WitherDragonEnum.Orange, WitherDragonEnum.Blue, WitherDragonEnum.Purple, WitherDragonEnum.Green)

        spawningDragons.sortBy { priorityList.indexOf(it) }

        if (totalPower >= WitherDragons.easyPower.value) {
            if (WitherDragons.soloDebuff.value == 1 && playerClass == DungeonClass.Tank && (spawningDragons.any { it == WitherDragonEnum.Purple } || WitherDragons.soloDebuffOnAll.value)) spawningDragons.sortByDescending {
                priorityList.indexOf(it)
            }
            else if (playerClass == DungeonClass.Healer && (spawningDragons.any { it == WitherDragonEnum.Purple } || WitherDragons.soloDebuffOnAll.value)) spawningDragons.sortByDescending {
                priorityList.indexOf(it)
            }
        }

        return spawningDragons[0]
    }
}