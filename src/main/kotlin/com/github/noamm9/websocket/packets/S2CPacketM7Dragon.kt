package com.github.noamm9.websocket.packets

import com.github.noamm9.features.impl.floor7.dragons.WitherDragonEnum
import com.github.noamm9.features.impl.floor7.dragons.WitherDragonState
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.websocket.PacketRegistry

class S2CPacketM7Dragon(val event: DragonEvent, val dragon: WitherDragonEnum): PacketRegistry.WebSocketPacket("m7dragon") {
    override fun handle() {
        ThreadUtils.scheduledTask {
            WitherDragonEnum.valueOf(dragon.name).let {
                when (event) {
                    DragonEvent.SPAWN -> {
                        if (it.state == WitherDragonState.ALIVE) return@let
                        it.state = WitherDragonState.ALIVE
                        it.timeToSpawn = 100
                        it.spawnedTime = DungeonListener.currentTime
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