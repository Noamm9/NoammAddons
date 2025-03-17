package noammaddons.features.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.SoundPlayEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.MathUtils.distance3D


object BatDeadTitle: Feature() {
    @SubscribeEvent
    fun onPacket(event: SoundPlayEvent) {
        if (! config.batDeadTitle) return
        if (event.name != "mob.bat.hurt") return
        if (distance3D(event.pos, mc.thePlayer.positionVector) > 25) return
        if (! inDungeon) return
        if (inBoss) return

        showTitle("§c§lBat Died")
    }
}
