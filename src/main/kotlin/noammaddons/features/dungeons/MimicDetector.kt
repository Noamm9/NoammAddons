package noammaddons.features.dungeons

import gg.essential.elementa.state.BasicState
import gg.essential.elementa.utils.withAlpha
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityZombie
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.RenderHelper.disableChums
import noammaddons.utils.RenderHelper.enableChums
import noammaddons.utils.RenderUtils
import noammaddons.utils.Utils.containsOneOf
import java.awt.Color


object MimicDetector: Feature() {
    val mimicKilled = BasicState(false)
    fun check() = ! mimicKilled.get() && inDungeon && (dungeonFloorNumber ?: 0) >= 6 && ! inBoss

    private fun sendMimicMessage() {
        if (! config.sendMimicKillMessage) return
        sendPartyMessage("$CHAT_PREFIX Mimic killed!")
        modMessage("&l&cMimic killed!")
        showTitle("&cMimic Dead!")
    }


    fun isMimic(entity: Entity): Boolean {
        if (entity is EntityZombie && entity.isChild) {
            for (i in 0 .. 3) {
                if (entity.getCurrentArmor(i) != null) return false
            }
            return true
        }
        else if (entity is EntityArmorStand && entity.hasCustomName()) {
            return entity.customNameTag.contains("§c§lMimic§r §e0§c❤")
        }
        return false
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        mimicKilled.set(false)
    }

    @SubscribeEvent
    fun onTick(event: Tick) {
        if (! check()) return
        if (mc.theWorld.loadedEntityList.filter { // @formatter:off
            it is EntityArmorStand || it is EntityZombie
        }.none { isMimic(it) }) return

        mimicKilled.set(true)
        sendMimicMessage()
    }


    @SubscribeEvent
    fun onEntityLeaveWorld(event: EntityLeaveWorldEvent) {
        if (! inDungeon) return
        if ((dungeonFloorNumber ?: 0) < 6) return
        if (mimicKilled.get()) return
        if (! isMimic(event.entity)) return
        mimicKilled.set(true)
        sendMimicMessage()
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! inDungeon) return
        if (inBoss) return
        if (! event.component.noFormatText.lowercase().containsOneOf(
            "skytils-dungeon-score-mimic",
            "mimic killed", "mimic slain",
            "mimic killed!", "mimic dead",
            "mimic dead!"
        )) return // @formatter:off

        mimicKilled.set(true)
    }

    @SubscribeEvent
    fun preChum(event: RenderChestEvent.Pre) {
        if (! config.highlightMimicChest) return
        if (event.chest.chestType != 1) return
        if (! check()) return
        enableChums(Color.WHITE)
    }

    @SubscribeEvent
    fun postChum(event: RenderChestEvent.Post) {
        if (! config.highlightMimicChest) return
        if (event.chest.chestType != 1) return
        if (! check()) return
        disableChums()
        RenderUtils.drawBlockBox(
            event.chest.pos,
            (if (config.disableVisualWords) Color.RED
            else Color.BLUE).withAlpha(40),
            outline = true, fill = true, phase = true
        )
    }
}