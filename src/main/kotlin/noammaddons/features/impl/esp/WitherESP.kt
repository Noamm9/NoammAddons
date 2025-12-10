package noammaddons.features.impl.esp

import net.minecraft.entity.boss.EntityWither
import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.network.play.server.S13PacketDestroyEntities
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ColorSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.EspUtils.espMob
import noammaddons.utils.LocationUtils
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.world
import java.awt.Color


object WitherESP: Feature("Highlights Withers in the world") {
    private val customColors = ToggleSetting("Custom Wither Colors").register1()
    private val maxorColor by ColorSetting("Maxor Color", Color(88, 4, 164), false).addDependency(customColors)
    private val stormColor by ColorSetting("Storm Color", Color(0, 208, 255), false).addDependency(customColors)
    private val goldorColor by ColorSetting("Goldor Color", Color(255, 255, 255), false).addDependency(customColors)
    private val necronColor by ColorSetting("Necron Color", Color(255, 0, 0), false).addDependency(customColors)
    private val vanquisherColor by ColorSetting("Vanquisher Color", Color(88, 4, 164), false).addDependency(customColors)

    enum class Wither(val defualtColor: Color) {
        MAXOR(Color(88, 4, 164)),
        STORM(Color(0, 208, 255)),
        GOLDOR(Color(255, 255, 255)),
        NECRON(Color(255, 0, 0)),
        VANQUISHER(MAXOR.defualtColor);

        fun getColor(): Color {
            return if (customColors.value) when (this) {
                MAXOR -> maxorColor
                STORM -> stormColor
                GOLDOR -> goldorColor
                NECRON -> necronColor
                VANQUISHER -> vanquisherColor
            }
            else this.defualtColor
        }

        companion object {
            var currentWither: EntityWither? = null
            var currentType: Wither = MAXOR

            fun reset() {
                currentWither = null
                currentType = MAXOR
            }

            fun updateType(bossName: String) {
                currentType = entries.find {
                    bossName.contains(it.name)
                } ?: return
            }
        }
    }

    private fun isValidLoc(): Boolean {
        return (dungeonFloorNumber == 7 && inBoss && F7Phase != 5) || world == LocationUtils.WorldType.CrimonIsle
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) = Wither.reset()


    /*
     The wither from the armor set: isArmored: false, invulTime: 800
     Maxor: isArmored: true, invulTime: 200
     Storm: isArmored: true, invulTime: 1
     Goldor: isArmored: true, invulTime: 0
     Necron: isArmored: true, invulTime: 1
     Vanquisher: isArmored: false/true, invulTime: 250
     */
    @SubscribeEvent
    fun onPacket(event: MainThreadPacketRecivedEvent.Post) {
        if (! isValidLoc()) return
        when (val packet = event.packet) {
            is S0FPacketSpawnMob -> {
                if (packet.entityType != 64) return // EntityWither
                val entity = mc.theWorld.getEntityByID(packet.entityID) as? EntityWither ?: return
                if (entity.isInvisible || entity.invulTime == 800) return
                Wither.currentWither = entity
            }

            is S13PacketDestroyEntities -> {
                packet.entityIDs.find { Wither.currentWither?.entityId == it }?.let {
                    Wither.currentWither = null
                }
            }
        }
    }

    @SubscribeEvent
    fun onBossbarUpdate(event: BossbarUpdateEvent.Pre) {
        if (! isValidLoc()) return
        Wither.updateType(event.bossName.removeFormatting().uppercase())
    }

    @SubscribeEvent
    fun onEntityRender(event: PostRenderEntityModelEvent) {
        if (! isValidLoc()) return
        if (event.entity != Wither.currentWither) return
        espMob(event.entity, Wither.currentType.getColor())
    }
}