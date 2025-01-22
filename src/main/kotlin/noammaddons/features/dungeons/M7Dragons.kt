package noammaddons.features.dungeons

import net.minecraft.entity.item.EntityItem
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraft.util.EnumParticleTypes.ENCHANTMENT_TABLE
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.MathUtils.distanceIn3DWorld
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getRenderVec
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawBox
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawTracer
import noammaddons.utils.SoundUtils
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color

object M7Dragons: Feature() {
    private var ticks = 0
    private var toggleTickCounter = false
    private var spawning = false
    private var redSpawning = false
    private var orangeSpawning = false
    private var blueSpawning = false
    private var purpleSpawning = false
    private var greenSpawning = false
    private var drags = arrayOfNulls<DragInfo?>(2)
    private var textColor = Color.WHITE
    private var currentPrio: DragInfo? = null
    private var arrowsHit = 0
    private var iceSprayHit = false
    private var listening = false
    private var arrowListener = false
    private var iceSprayListener = false

    private data class DragInfo(
        val color: Color,
        val prio: IntArray,
        val stateCoords: Vec3,
        val stateBox: Pair<Vec3, Vec3>
    )

    private val dragInfo = mapOf(
        "purple" to DragInfo(
            Healer.color,
            intArrayOf(0, 4),
            Vec3(54.0, 17.0, 122.0),
            Pair(
                Vec3(45.5, 13.0, 113.5),
                Vec3(68.5, 23.0, 136.5)
            )
        ),
        "blue" to DragInfo(
            Mage.color,
            intArrayOf(1, 0),
            Vec3(84.0, 19.0, 97.0),
            Pair(
                Vec3(71.5, 16.0, 82.5),
                Vec3(96.5, 26.0, 107.5)
            )
        ),
        "red" to DragInfo(
            Archer.color,
            intArrayOf(2, 1),
            Vec3(28.0, 16.0, 57.0),
            Pair(
                Vec3(14.5, 13.0, 45.5),
                Vec3(39.5, 28.0, 70.5)
            )
        ),
        "green" to DragInfo(
            Tank.color,
            intArrayOf(3, 2),
            Vec3(28.0, 16.0, 92.0),
            Pair(
                Vec3(7.0, 8.0, 80.0),
                Vec3(37.0, 28.0, 110.0)
            )
        ),
        "orange" to DragInfo(
            Berserk.color,
            intArrayOf(4, 3),
            Vec3(83.0, 16.0, 58.0),
            Pair(
                Vec3(72.0, 8.0, 47.0),
                Vec3(102.0, 28.0, 77.0)
            )
        )
    )

    @SubscribeEvent
    fun tickCounter(event: PacketEvent.Received) {
        if (config.M7dragonsSpawnTimer || config.M7dragonsShowDebuff || config.M7dragonsKillBox) {
            if (F7Phase != 5) return
            val packet = event.packet

            if (packet is S32PacketConfirmTransaction) {
                if (! toggleTickCounter) return
                ticks --
                if (ticks <= 0) {
                    toggleTickCounter = false
                    spawning = false
                }
            }

            if (packet !is S2APacketParticles) return
            if (packet.particleType != ENCHANTMENT_TABLE) return

            handleParticles(
                packet.xCoordinate.toInt(),
                packet.yCoordinate.toInt(),
                packet.zCoordinate.toInt()
            )
        }
    }


    @SubscribeEvent
    fun drawTimer(event: RenderOverlay) {
        if (F7Phase != 5) return
        if (! config.M7dragonsSpawnTimer || ticks <= 0) return

        drawCenteredText(
            "${ticks / 20.0}".toFixed(2),
            mc.getWidth() / 2f,
            mc.getHeight() * 0.4f,
            3f, textColor,
        )
    }

    @SubscribeEvent
    fun drawTrace(event: RenderWorld) {
        if (currentPrio == null) return
        if (F7Phase != 5) return

        if (ticks > 0 && config.M7dragonsSpawnTimer) drawTracer(
            currentPrio !!.stateCoords,
            textColor
        )

        if (config.M7dragonsKillBox) drawBox(
            currentPrio !!.stateBox.first,
            currentPrio !!.stateBox.second,
            currentPrio !!.color,
            outline = true,
            fill = false,
            phase = true
        )
    }

    /*
    @SubscribeEvent
    fun onDeath(event: MessageSentEvent) {

        assignColor(dragInfo["blue"]!!)
        ticks = 100
        toggleTickCounter = true
        blueSpawning = true
        setTimeout(8000) { blueSpawning = false }

        assignColor(dragInfo["orange"]!!)
        ticks = 100
        toggleTickCounter = true
        orangeSpawning = true
        setTimeout(8000) { orangeSpawning = false }
    }
    */

    @SubscribeEvent
    fun reset(event: WorldEvent.Unload) {
        ticks = 0
        redSpawning = false
        orangeSpawning = false
        blueSpawning = false
        purpleSpawning = false
        greenSpawning = false
        toggleTickCounter = false
        drags = arrayOfNulls(2)
        currentPrio = null
    }


    private fun assignColor(drag: DragInfo) {
        if (drags[0] == null) drags[0] = drag
        else if (drags[1] == null && drag != drags[0]) {
            drags[1] = drag
            determinePrio()
        }
        else {
            currentPrio = drag
            textColor = currentPrio !!.color
        }
    }

    private fun determinePrio() {
        when (thePlayer?.clazz ?: return) {
            Archer, Tank -> {
                currentPrio = if (drags[0] !!.prio[0] < drags[1] !!.prio[0]) drags[0] !!
                else drags[1] !!
                textColor = currentPrio !!.color
            }

            Berserk, Mage, Healer -> {
                //	if (config.healerTeam == 1) {
                currentPrio = if (drags[0] !!.prio[0] > drags[1] !!.prio[0]) drags[0] !!
                else drags[1] !!
                textColor = currentPrio !!.color
                //}
            }/*
			"Healer" -> {
				if (config.healerTeam == 0) {
					if (drags[0]!!.prio[1] < drags[1]!!.prio[1]) {
						timeText.setColor(drags[0]!!.color)
					} else {
						timeText.setColor(drags[1]!!.color)
					}
				}
			}*/
        }
    }

    private fun handleParticles(x: Int, y: Int, z: Int) {
        if (y in 14 .. 19) {
            when (x) {
                in 27 .. 32 -> {
                    when (z) {
                        59 -> if (! redSpawning) {
                            debuffListener()
                            listening = true
                            assignColor(dragInfo["red"] !!)
                            ticks = 100
                            toggleTickCounter = true
                            redSpawning = true
                            SoundUtils.Pling.start()
                            setTimeout(8000) { redSpawning = false }
                        }

                        94 -> if (! greenSpawning) {
                            debuffListener()
                            listening = true
                            assignColor(dragInfo["green"] !!)
                            ticks = 100
                            toggleTickCounter = true
                            greenSpawning = true
                            SoundUtils.Pling.start()
                            setTimeout(8000) { greenSpawning = false }
                        }
                    }
                }

                in 79 .. 85 -> {
                    when (z) {
                        94 -> if (! blueSpawning) {
                            debuffListener()
                            listening = true
                            assignColor(dragInfo["blue"] !!)
                            ticks = 100
                            toggleTickCounter = true
                            blueSpawning = true
                            SoundUtils.Pling.start()
                            setTimeout(8000) { blueSpawning = false }
                        }

                        56 -> if (! orangeSpawning) {
                            debuffListener()
                            listening = true
                            assignColor(dragInfo["orange"] !!)
                            ticks = 100
                            toggleTickCounter = true
                            orangeSpawning = true
                            SoundUtils.Pling.start()
                            setTimeout(8000) { orangeSpawning = false }
                        }
                    }
                }

                56 -> {
                    if (! purpleSpawning) {
                        debuffListener()
                        listening = true
                        assignColor(dragInfo["purple"] !!)
                        ticks = 100
                        toggleTickCounter = true
                        purpleSpawning = true
                        SoundUtils.Pling.start()
                        setTimeout(8000) { purpleSpawning = false }
                    }
                }
            }
        }
    }

    private fun debuffListener() {
        if (! config.M7dragonsShowDebuff) return
        if (listening) return
        arrowsHit = 0
        iceSprayHit = false
        listening = true
        arrowListener = true
        iceSprayListener = true

        setTimeout(7_500) {
            arrowListener = false
            iceSprayListener = false
            listening = false

            modMessage(
                """
				Arrows Hit: $arrowsHit. ${
                    if (thePlayer?.clazz?.name.equalsOneOf("Tank", "Mage", "Healer"))
                        "Ice Spray: ${if (iceSprayHit) "Yes" else "No"}"
                    else ""
                }""".trimIndent()
            )
        }
    }

    @SubscribeEvent
    fun onSoundPlay(event: SoundPlayEvent) {
        if (! arrowListener) return
        if (event.name != "random.successful_hit") return
        arrowsHit ++
    }

    @SubscribeEvent
    fun onIceSpray(event: Tick) {
        if (! iceSprayListener) return

        mc.theWorld.loadedEntityList
            .filterIsInstance<EntityItem>()
            .filter { it.name == "item.tile.ice" }
            .forEach {
                if (distanceIn3DWorld(
                        Player !!.getRenderVec(),
                        it.getRenderVec()
                    ) <= 25
                ) iceSprayHit = true
            }
    }

    /*
        @SubscribeEvent
        fun idk(event: LivingDeathEvent) {
            if (! listening) return

            if (event.entity is EntityDragon) {
                if (distanceIn2DWorld(event.entity.positionVector, currentPrio !!.stateCoords) > 17) return
                val inBox = MathUtils.isCoordinateInsideBox(
                    event.entity.positionVector,
                    currentPrio !!.stateBox.first,
                    currentPrio !!.stateBox.second
                )

                modMessage(
                    if (inBox) "&bDragon has been killed successfully. &aGG!"
                    else "&bDragon Skip failed. &4What a loser"
                )

                showTitle(
                    subtitle = if (inBox) "&aDragon Skip Successful"
                    else "&4Dragon Skip Failed",
                    time = 1.5f
                )

                currentPrio = null
            }
        }*/
}



