package noammaddons.features.dungeons

import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.item.EntityItem
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.client.event.sound.PlaySoundEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noammaddons.events.PacketEvent
import noammaddons.events.RenderOverlay
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.equalsOneOf
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.DungeonUtils.Classes
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.MathUtils
import noammaddons.utils.MathUtils.distanceIn2DWorld
import noammaddons.utils.MathUtils.distanceIn3DWorld
import noammaddons.utils.MathUtils.toFixed
import noammaddons.utils.RenderUtils.drawBox
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawTracer
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth
import noammaddons.utils.ThreadUtils.setTimeout
import java.awt.Color

object M7Dragons {
	private data class DragInfo(
		val color: Color,
		val prio: IntArray,
		val stateCoords: Vec3,
		val stateBox: Pair<Vec3, Vec3>
	)
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
	private val dragInfo = mapOf(
		"purple" to DragInfo(
			Classes.Healer.color,
			intArrayOf(0, 4),
			Vec3(54.0, 17.0, 122.0),
			Pair(
				Vec3(45.5, 13.0, 113.5),
				Vec3(68.5, 23.0, 136.5)
			)
		),
		"blue" to DragInfo(
			Classes.Mage.color,
			intArrayOf(1, 0),
			Vec3(84.0, 19.0, 97.0),
			Pair(
				Vec3(71.5, 16.0, 82.5),
				Vec3(96.5, 26.0, 107.5)
			)
		),
		"red" to DragInfo(
			Classes.Archer.color,
			intArrayOf(2, 1),
			Vec3(28.0, 16.0, 57.0),
			Pair(
				Vec3(14.5, 13.0, 45.5),
				Vec3(39.5, 28.0, 70.5)
			)
		),
		"green" to DragInfo(
			Classes.Tank.color,
			intArrayOf(3, 2),
			Vec3(28.0, 16.0, 92.0),
			Pair(
				Vec3(7.0, 8.0, 80.0),
				Vec3(37.0, 28.0, 110.0)
			)
		),
		"orange" to DragInfo(
			Classes.Berserk.color,
			intArrayOf(4, 3),
			Vec3(83.0, 16.0, 58.0),
			Pair(
				Vec3(72.0, 8.0, 47.0),
				Vec3(102.0, 28.0, 77.0)
			)
		)
	)
	private var arrowsHit = 0
	private var iceSprayHit = false
	private var listening = false
	private var arrowListener = false
	private var iceSprayListener = false
	
	
	@SubscribeEvent
	fun tickCounter(event: PacketEvent.Received) {
		if (!config.M7dragons) return
		
		if (event.packet is S32PacketConfirmTransaction) {
			if (!toggleTickCounter) return
			ticks--
			if (ticks <= 0) {
				toggleTickCounter = false
				spawning = false
			}
		}

		if (event.packet is S2APacketParticles) {
			if (F7Phase == 5 && event.packet.particleType.toString() == "ENCHANTMENT_TABLE") {
				handleParticles(
					event.packet.xCoordinate.toInt(),
					event.packet.yCoordinate.toInt(),
					event.packet.zCoordinate.toInt()
				)
			}
		}
	}
	
	
	@SubscribeEvent
	fun drawTimer(event: RenderOverlay) {
		if (F7Phase != 5) return
		if (!config.M7dragons || ticks <= 0) return
		val timeLeft = (ticks / 20.0).toFixed(2)
		
		drawCenteredText(
			timeLeft,
			mc.getWidth()/2f,
			mc.getHeight()*0.4f,
			3f, textColor,
		)
	}
	
	@SubscribeEvent
	fun drawTrace(event: RenderWorldLastEvent) {
		if (!config.M7dragons || currentPrio == null) return
		if (F7Phase != 5) return
		
		if (ticks > 0) {
			drawTracer(currentPrio!!.stateCoords, textColor)
		}
		
		drawBox(
			currentPrio!!.stateBox.first,
			currentPrio!!.stateBox.second,
			currentPrio!!.color,
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
	@Suppress("UNUSED_PARAMETER")
	fun reset(event: WorldEvent.Unload) {
		ticks = 0
		redSpawning = false
		orangeSpawning = false
		blueSpawning = false
		purpleSpawning = false
		greenSpawning = false
		toggleTickCounter = false
		drags = arrayOfNulls(2)
	}
	
	
	private fun assignColor(drag: DragInfo) {
		if (drags[0] == null) drags[0] = drag
		else if (drags[1] == null && drag != drags[0]) {
			drags[1] = drag
			determinePrio()
		}
		else {
			currentPrio = drag
			textColor = currentPrio!!.color
		}
	}
	
	private fun determinePrio() {
		when (thePlayer?.clazz?.name ?: return) {
			"Archer", "Tank" -> {
				currentPrio = if (drags[0]!!.prio[0] < drags[1]!!.prio[0]) drags[0]!!
				else drags[1]!!
				textColor = currentPrio!!.color
			}
			"Berserk", "Mage", "Healer" -> {
			//	if (config.healerTeam == 1) {
				currentPrio = if (drags[0]!!.prio[0] > drags[1]!!.prio[0]) drags[0]!!
				else drags[1]!!
				textColor = currentPrio!!.color
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
		if (y in 14..19) {
			when (x) {
				in 27..32 -> {
					when (z) {
						59 -> if (!redSpawning) {
							debuffListener()
							listening = true
							assignColor(dragInfo["red"]!!)
							ticks = 100
							toggleTickCounter = true
							redSpawning = true
							setTimeout(8000) { redSpawning = false }
						}
						
						94 -> if (!greenSpawning) {
							debuffListener()
							listening = true
							assignColor(dragInfo["green"]!!)
							ticks = 100
							toggleTickCounter = true
							greenSpawning = true
							setTimeout(8000) { greenSpawning = false }
						}
					}
				}
				in 79..85 -> {
					when (z) {
						94 -> if (!blueSpawning) {
							debuffListener()
							listening = true
							assignColor(dragInfo["blue"]!!)
							ticks = 100
							toggleTickCounter = true
							blueSpawning = true
							setTimeout(8000) { blueSpawning = false }
						}
						
						56 -> if (!orangeSpawning) {
							debuffListener()
							listening = true
							assignColor(dragInfo["orange"]!!)
							ticks = 100
							toggleTickCounter = true
							orangeSpawning = true
							setTimeout(8000) { orangeSpawning = false }
						}
					}
				}
				56 -> {
					if (!purpleSpawning) {
						debuffListener()
						listening = true
						assignColor(dragInfo["purple"]!!)
						ticks = 100
						toggleTickCounter = true
						purpleSpawning = true
						setTimeout(8000) { purpleSpawning = false }
					}
				}
			}
		}
	}

	

	private fun debuffListener() {
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
			
			modMessage("""
				${CHAT_PREFIX.removeFormatting()} Arrows Hit: $arrowsHit. ${
					if (thePlayer?.clazz?.name.equalsOneOf("Tank", "Mage", "Healer"))
						"Ice Spray: ${if (iceSprayHit) "Yes" else "No"}"
					else ""
				}""".trimIndent()
			)
		}
	}
	
	@SubscribeEvent
	fun onSoundPlay(event: PlaySoundEvent) {
		if (!arrowListener) return
		if (event.name != "random.successful_hit") return
		
		arrowsHit++
	}
	
	@SubscribeEvent
	fun onIceSpray(event: ClientTickEvent) {
		if (!iceSprayListener) return
		
		mc.theWorld.loadedEntityList
			.filter { entity -> entity::class.java == EntityItem::class.java }
			.filter { type -> type?.name == "item.tile.ice" }
			.forEach { item ->
				if (item.posX <= 30 && item.posY >= 10 &&
				    distanceIn3DWorld(
					    mc.thePlayer.positionVector,
					    item.positionVector
					) <= 25
				)
				{ iceSprayHit = true }
			}
	}
	
	@SubscribeEvent
	fun idk(event: LivingDeathEvent) {
		if (!listening) return
		
		if (event.entity is EntityDragon) {
			if (distanceIn2DWorld(event.entity.positionVector, currentPrio!!.stateCoords) > 17) return
			val inBox = MathUtils.isCoordinateInsideBox(
				event.entity.positionVector,
				currentPrio!!.stateBox.first,
				currentPrio!!.stateBox.second
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
	}
}



