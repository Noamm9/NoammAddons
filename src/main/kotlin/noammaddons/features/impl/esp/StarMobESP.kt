package noammaddons.features.impl.esp

import net.minecraft.entity.Entity
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.network.play.server.S1CPacketEntityMetadata
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PostPacketEvent
import noammaddons.events.RenderEntityEvent
import noammaddons.features.Feature
import noammaddons.features.impl.dungeons.FpsBoost.dungeonMobRegex
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.EspUtils.espMob
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.ThreadUtils.scheduledTask
import noammaddons.utils.Utils.favoriteColor
import java.awt.Color


object StarMobESP: Feature("Highlights Star Mobs in the dungoen") {
    val checked = HashSet<Entity>()

    @JvmField
    val starMobs = HashSet<Entity>()

    private val espBats = ToggleSetting("Hightlight Bats")
    private val espFels = ToggleSetting("Hightlight Fels")
    private val customMinibossesColors = ToggleSetting("Custom Minibosses Colors")

    private val starMobColor = ColorSetting("Star Mob Color", favoriteColor, false)
    private val felColor = ColorSetting("Fel Color", Color.pink, false)
    private val batColor = ColorSetting("Bat Color", Color.GREEN, false)

    private val shadowAssasianColor = ColorSetting("Shadow Assasian", Color.BLACK, false).addDependency(customMinibossesColors)
    private val angryArchaeologistColor = ColorSetting("Angry Archaeologist", favoriteColor, false).addDependency(customMinibossesColors)
    private val unstableDragonColor = ColorSetting("Unstable Dragon", Color(100, 0, 100), false).addDependency(customMinibossesColors)
    private val youngDragonColor = ColorSetting("Young Dragon", Color.WHITE.darker(), false).addDependency(customMinibossesColors)
    private val superiorDragonColor = ColorSetting("Superior Dragon", Color.YELLOW, false).addDependency(customMinibossesColors)
    private val holyDragonColor = ColorSetting("Holy Dragon", Color.GREEN, false).addDependency(customMinibossesColors)
    private val frozenAdventurerColor = ColorSetting("Frozen Adventurer", Color.CYAN, false).addDependency(customMinibossesColors)

    override fun init() = addSettings(
        espBats, espFels, customMinibossesColors,
        SeperatorSetting("Colors"),
        starMobColor, felColor, batColor,
        shadowAssasianColor, angryArchaeologistColor,
        unstableDragonColor, youngDragonColor,
        superiorDragonColor, holyDragonColor,
        frozenAdventurerColor
    )

    init {
        onWorldLoad {
            starMobs.clear()
            checked.clear()
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onPacket(event: PostPacketEvent.Received) {
        if (! inDungeon) return
        if (inBoss) return

        scheduledTask(0) { // execute on next tick
            when (val packet = event.packet) {
                is S1CPacketEntityMetadata -> {
                    val armorStand = mc.theWorld.getEntityByID(packet.entityId) as? EntityArmorStand ?: return@scheduledTask
                    if (! armorStand.hasCustomName()) return@scheduledTask
                    if (! armorStand.customNameTag.contains("§6✯")) return@scheduledTask
                    checkStarMob(armorStand)
                }

                is S0FPacketSpawnMob -> {
                    if (packet.entityType != 30) return@scheduledTask
                    val nameData = packet.func_149027_c()?.find { it.getObject().toString().contains("§") } ?: return@scheduledTask
                    val name = "${nameData.getObject()}"
                    if (! name.matches(dungeonMobRegex) || ! name.contains("§6✯")) return@scheduledTask
                    val armorStand = mc.theWorld.getEntityByID(packet.entityID) as EntityArmorStand
                    checkStarMob(armorStand)
                }
            }

        }
    }

    @SubscribeEvent
    fun onRenderEntity(event: RenderEntityEvent) {
        if (! inDungeon) return
        if (inBoss) return

        espMob(
            event.entity,
            if (event.entity in starMobs) starMobColor.value
            else getColor(event.entity) ?: return
        )
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
        possibleEntities.find {
            ! starMobs.contains(it) && when (it) {
                is EntityPlayer -> ! it.isInvisible() && it.getUniqueID()
                    .version() == 2 && it != mc.thePlayer

                is EntityWither -> false
                else -> true
            }
        }?.let {
            if (getColor(it) == null) starMobs.add(it)
            checked.add(armorStand)
        }
    }

    @JvmStatic
    fun getColor(entity: Entity) = when (entity) {
        is EntityBat -> if (espBats.value && ! entity.isInvisible) batColor else null
        is EntityEnderman -> if (espFels.value && entity.name == "Dinnerbone") felColor else null
        is EntityPlayer -> when {
            entity.name.contains("Shadow Assassin") -> if (customMinibossesColors.value) shadowAssasianColor else starMobColor

            entity.getCurrentArmor(0) != null && (dungeonFloorNumber == 4 && ! inBoss) -> when (entity.name) {
                "Lost Adventurer" -> if (customMinibossesColors.value) when (entity.getCurrentArmor(0).displayName) {
                    "§6Unstable Dragon Boots" -> unstableDragonColor
                    "§6Young Dragon Boots" -> youngDragonColor
                    "§6Superior Dragon Boots" -> superiorDragonColor
                    "§6Holy Dragon Boots" -> holyDragonColor
                    "§6Frozen Blaze Boots" -> frozenAdventurerColor
                    else -> null
                }
                else starMobColor

                "Diamond Guy" -> if (customMinibossesColors.value &&
                    entity.getCurrentArmor(0).displayName.startsWith("§6Perfect Boots - Tier")
                ) angryArchaeologistColor
                else starMobColor

                else -> null
            }

            else -> null
        }

        else -> null
    }?.value

}