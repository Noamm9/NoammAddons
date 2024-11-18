package noammaddons.features.dungeons.ESP

import net.minecraft.entity.Entity
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderEntityModelEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.EspUtils.EspMob
import noammaddons.utils.LocationUtils.dungeonFloor
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.RenderUtils.drawEntityBox
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color

object MobESP: Feature() {
    private val checked = HashSet<Entity>()
    val starMobs = HashSet<Entity>()

    @SubscribeEvent
    fun onRenderEntity(event: RenderEntityModelEvent) {
        if (! inDungeons) return
        if (event.entity is EntityArmorStand) {
            if (config.espStarMobs && event.entity.hasCustomName() &&
                event.entity.customNameTag.contains("✯") && ! checked.contains(event.entity)
            ) {
                val possibleEntities = event.entity.entityWorld.getEntitiesInAABBexcluding(
                    event.entity, event.entity.entityBoundingBox.offset(0.0, - 1.0, 0.0)
                ) { it !is EntityArmorStand }
                possibleEntities.find {
                    ! starMobs.contains(it) && when (it) {
                        is EntityPlayer -> ! it.isInvisible() && it.getUniqueID()
                            .version() == 2 && it != Player

                        is EntityWither -> false
                        else -> true
                    }
                }?.let {
                    if (getColor(it) == null && it !is EntityBat) starMobs.add(it)
                    if (config.removeStarMobsNametag) {
                        mc.theWorld.removeEntity(event.entity)
                    }
                    else checked.add(event.entity)
                }
            }
            return
        }

        if (! config.espType.equalsOneOf(0, 2)) return
        if (inBoss) return

        EspMob(event, if (event.entity in starMobs) config.espColorStarMobs else getColor(event.entity) ?: return)
    }

    @SubscribeEvent
    fun renderBoxs(event: RenderWorld) {
        if (config.espType != 1) return
        if (inBoss) return
        mc.theWorld.loadedEntityList.forEach {

            if (starMobs.contains(it)) drawEntityBox(it, config.espColorStarMobs)
            else drawEntityBox(it, getColor(it) ?: return@forEach)
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        starMobs.clear()
        checked.clear()
    }

    fun getColor(entity: Entity): Color? {
        return when (entity) {
            is EntityBat -> if (config.espBats && ! entity.isInvisible) config.espColorBats else null
            is EntityEnderman -> if (config.espFels && entity.name == "Dinnerbone") config.espColorFels else null
            is EntityPlayer -> when {
                (config.espShadowAssassin || config.espMiniboss) && entity.name.contains("Shadow Assassin") -> config.espColorShadowAssassin

                config.espMiniboss && entity.getCurrentArmor(0) != null && dungeonFloor != 4 && inBoss -> when (entity.name) {
                    "Lost Adventurer" -> if (config.espSeperateMinibossColor) when (entity.getCurrentArmor(0).displayName) {
                        "§6Unstable Dragon Boots" -> config.espColorUnstable
                        "§6Young Dragon Boots" -> config.espColorYoung
                        "§6Superior Dragon Boots" -> config.espColorSuperior
                        "§6Holy Dragon Boots" -> config.espColorHoly
                        "§6Frozen Blaze Boots" -> config.espColorFrozen
                        else -> null
                    }
                    else config.espColorMiniboss

                    "Diamond Guy" -> if (config.espSeperateMinibossColor &&
                        entity.getCurrentArmor(0).displayName.startsWith("§6Perfect Boots - Tier")
                    ) config.espColorAngryArchaeologist
                    else config.espColorMiniboss

                    else -> null
                }

                else -> null
            }

            else -> null
        }
    }

}