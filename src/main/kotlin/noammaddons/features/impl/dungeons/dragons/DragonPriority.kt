package noammaddons.features.impl.dungeons.dragons

import noammaddons.features.impl.dungeons.dragons.WitherDragonEnum.*
import noammaddons.features.impl.dungeons.dragons.WitherDragons.dragonPriorityToggle
import noammaddons.features.impl.dungeons.dragons.WitherDragons.dragonTitle
import noammaddons.features.impl.dungeons.dragons.WitherDragons.easyPower
import noammaddons.features.impl.dungeons.dragons.WitherDragons.normalPower
import noammaddons.features.impl.dungeons.dragons.WitherDragons.soloDebuff
import noammaddons.features.impl.dungeons.dragons.WitherDragons.soloDebuffOnAll
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.DungeonUtils.Blessing.*
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.Utils.equalsOneOf

object DragonPriority {
    fun findPriority(spawningDragons: MutableList<WitherDragonEnum>): WitherDragonEnum {
        return if (! dragonPriorityToggle) {
            spawningDragons.sortBy { listOf(Red, Orange, Blue, Purple, Green).indexOf(it) }
            spawningDragons[0]
        }
        else sortPriority(spawningDragons)
    }

    fun displaySpawningDragon(dragon: WitherDragonEnum) {
        if (dragon == None) return
        if (dragonTitle && WitherDragons.enabled) showTitle("&${dragon.colorCode}${dragon.name}", time = 1.5)
    }

    private fun sortPriority(spawningDragons: MutableList<WitherDragonEnum>): WitherDragonEnum {
        val totalPower = POWER.current + (if (TIME.current > 0) 2.5 else 0.0)
        val playerClass = thePlayer?.clazz

        val dragonList = listOf(Orange, Green, Red, Blue, Purple)
        val priorityList =
            if (totalPower >= normalPower || (spawningDragons.any { it == Purple } && totalPower >= easyPower))
                if (playerClass.equalsOneOf(Berserk, Mage)) dragonList else dragonList.reversed()
            else listOf(Red, Orange, Blue, Purple, Green)

        spawningDragons.sortBy { priorityList.indexOf(it) }

        if (totalPower >= easyPower) {
            if (soloDebuff == 1 && playerClass == Tank && (spawningDragons.any { it == Purple } || soloDebuffOnAll)) spawningDragons.sortByDescending {
                priorityList.indexOf(it)
            }
            else if (playerClass == Healer && (spawningDragons.any { it == Purple } || soloDebuffOnAll)) spawningDragons.sortByDescending {
                priorityList.indexOf(it)
            }
        }

        return spawningDragons[0]
    }
}