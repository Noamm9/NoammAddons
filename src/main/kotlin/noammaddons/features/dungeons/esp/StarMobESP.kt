package noammaddons.features.dungeons.esp

import net.minecraft.entity.Entity
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PostRenderEntityModelEvent
import noammaddons.events.RenderWorld
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.EspUtils.EspMob
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.RenderUtils.drawEntityBox
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color


object StarMobESP: Feature() {
    val checked = HashSet<Entity>()

    @JvmField
    val starMobs = HashSet<Entity>()

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderNameTag(event: RenderLivingEvent.Specials.Pre<EntityArmorStand>) {
        if (! config.espStarMobs) return
        if (! inDungeon) return
        val armorStand = event.entity as? EntityArmorStand ?: return
        if (! armorStand.hasCustomName()) return
        if (! armorStand.customNameTag.contains("§6✯")) return
        checkStarMob(armorStand)
    }


    @SubscribeEvent
    fun onRenderEntity(event: PostRenderEntityModelEvent) {
        if (! config.espStarMobs) return
        if (! inDungeon) return
        if (! config.espType.equalsOneOf(0, 2)) return
        if (inBoss) return

        EspMob(
            event,
            if (event.entity in starMobs) config.espColorStarMobs
            else getColor(event.entity) ?: return
        )
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! config.espStarMobs) return
        if (! inDungeon) return
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

    fun checkStarMob(armorStand: EntityArmorStand) {
        if (checked.contains(armorStand)) return
        val name = armorStand.customNameTag.removeFormatting().uppercase()
        // withermancers are always -3 to real entity the -1 and -2 are the wither skulls that they shoot
        val id = if (name.contains("WITHERMANCER")) 3 else 1

        val mob = armorStand.entityWorld.getEntityByID(armorStand.entityId - id)
        if (mob !is EntityArmorStand && mob !in starMobs) {
            starMobs.add(mob)
            checked.add(armorStand)
            return
        }

        val possibleEntities = armorStand.entityWorld.getEntitiesInAABBexcluding(
            armorStand, armorStand.entityBoundingBox.offset(0.0, - 1.0, 0.0)
        ) { it !is EntityArmorStand }

        possibleEntities.filter {
            ! starMobs.contains(it) && when (it) {
                is EntityPlayer -> ! it.isInvisible() && it.getUniqueID().version() == 2 && it != mc.thePlayer
                is EntityWither -> false
                else -> true
            }
        }.forEach {
            if (getColor(it) == null && it !is EntityBat) starMobs.add(it)
            checked.add(armorStand)
        }
    }


    @JvmStatic
    fun getColor(entity: Entity): Color? {
        return when (entity) {
            is EntityBat -> if (config.espBats && ! entity.isInvisible) config.espColorBats else null
            is EntityEnderman -> if (config.espFels && entity.name == "Dinnerbone") config.espColorFels else null
            is EntityPlayer -> when {
                (config.espShadowAssassin || config.espMiniboss) && entity.name.contains("Shadow Assassin") -> config.espColorShadowAssassin

                config.espMiniboss && entity.getCurrentArmor(0) != null && dungeonFloorNumber != 4 && inBoss -> when (entity.name) {
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