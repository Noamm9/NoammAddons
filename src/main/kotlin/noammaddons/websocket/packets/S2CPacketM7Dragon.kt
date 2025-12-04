package noammaddons.websocket.packets

import noammaddons.NoammAddons.Companion.mc
import noammaddons.features.impl.dungeons.dragons.WitherDragonEnum
import noammaddons.features.impl.dungeons.dragons.WitherDragonEnum.Companion.WitherDragonState
import noammaddons.features.impl.dungeons.dragons.WitherDragons
import noammaddons.websocket.PacketRegistry

class S2CPacketM7Dragon(val event: DragonEvent, val dragon: WitherDragonEnum): PacketRegistry.WebSocketPacket("m7dragon") {
    override fun handle() {
        mc.addScheduledTask {
            WitherDragonEnum.valueOf(dragon.name).let {
                when (event) {
                    DragonEvent.SPAWN -> {
                        if (it.state == WitherDragonState.ALIVE) return@let
                        it.state = WitherDragonState.ALIVE
                        it.timeToSpawn = 100
                        it.spawnedTime = WitherDragons.currentTick
                        it.sprayedTime = null
                        it.arrowsHit = 0
                    }

                    DragonEvent.DEATH -> it.setDead()
                }
            }
        }
    }

    enum class DragonEvent { SPAWN, DEATH }
}