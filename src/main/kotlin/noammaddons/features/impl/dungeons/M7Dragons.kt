package noammaddons.features.impl.dungeons

import net.minecraft.entity.item.EntityItem
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.DungeonUtils.Classes.*
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.MathUtils.distance3D
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils.drawBox
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawTracer
import noammaddons.utils.SoundUtils
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color

object M7Dragons: Feature(_name = "M7 Dragons", desc = "Prio, kill-box, spawn timer and more.") {
    val dragonSpawnTimer by ToggleSetting("Dragon Spawn Timer")
    val showDebuff by ToggleSetting("Debuff Info")
    val dragonsKillBox by ToggleSetting("Kill Box", true)

    data class DragInfo(val color: Color, val prio: IntArray, val stateCoords: Vec3, val stateBox: Pair<Vec3, Vec3>)

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

    init {
        onServerTick({ toggleTickCounter }) {
            if (F7Phase != 5) return@onServerTick

            ticks --
            if (ticks <= 0) {
                toggleTickCounter = false
                spawning = false
            }
        }

        onPacket<S2APacketParticles>({ dragonSpawnTimer && F7Phase == 5 }) { packet ->
            handleParticles(
                packet.xCoordinate.toInt(),
                packet.yCoordinate.toInt(),
                packet.zCoordinate.toInt()
            )
        }

        onWorldLoad {
            ticks = 0
            toggleTickCounter = false
            spawning = false
            redSpawning = false
            orangeSpawning = false
            blueSpawning = false
            purpleSpawning = false
            greenSpawning = false
            drags = arrayOfNulls<DragInfo?>(2)
            textColor = Color.WHITE
            currentPrio = null
            arrowsHit = 0
            iceSprayHit = false
            listening = false
            arrowListener = false
            iceSprayListener = false
        }
    }


    @SubscribeEvent
    fun drawTimer(event: RenderOverlay) {
        if (F7Phase != 5) return
        if (! dragonSpawnTimer || ticks <= 0) return

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

        if (ticks > 0 && dragonSpawnTimer) drawTracer(
            currentPrio !!.stateCoords,
            textColor
        )

        if (dragonsKillBox) drawBox(
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
            else -> return
        }
    }

    private fun handleParticles(x: Int, y: Int, z: Int) {
        if (y !in 14 .. 19) return

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
                        SoundUtils.Pling()
                        setTimeout(8000) { redSpawning = false }
                    }

                    94 -> if (! greenSpawning) {
                        debuffListener()
                        listening = true
                        assignColor(dragInfo["green"] !!)
                        ticks = 100
                        toggleTickCounter = true
                        greenSpawning = true
                        SoundUtils.Pling()
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
                        SoundUtils.Pling()
                        setTimeout(8000) { blueSpawning = false }
                    }

                    56 -> if (! orangeSpawning) {
                        debuffListener()
                        listening = true
                        assignColor(dragInfo["orange"] !!)
                        ticks = 100
                        toggleTickCounter = true
                        orangeSpawning = true
                        SoundUtils.Pling()
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
                    SoundUtils.Pling()
                    setTimeout(8000) { purpleSpawning = false }
                }
            }
        }

    }

    private fun debuffListener() {
        if (! showDebuff) return
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
                if (distance3D(mc.thePlayer.renderVec, it.renderVec) > 25) return@forEach
                iceSprayHit = true
            }
    }
}



