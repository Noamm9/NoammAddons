package noammaddons.features.impl.dungeons.dragons

import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.MessageSentEvent
import noammaddons.features.impl.dungeons.dragons.DragonPriority.displaySpawningDragon
import noammaddons.features.impl.dungeons.dragons.DragonPriority.findPriority
import noammaddons.features.impl.dungeons.dragons.WitherDragonEnum.Companion.dragonSpawnCount
import noammaddons.features.impl.dungeons.dragons.WitherDragons.priorityDragon
import noammaddons.utils.ChatUtils.modMessage

object DragonTest {
    @SubscribeEvent
    fun onMessage(event: MessageSentEvent) {
        if (! event.message.startsWith("/")) return
        val parts = event.message.substring(1).split(" ")
        val command = parts.firstOrNull()?.lowercase() ?: return
        val args = parts.drop(1)

        when (command) {
            "simdrag" -> {
                event.isCanceled = true

                if (args.isEmpty()) return modMessage("Usage: /simdrag <dragon_name> [another_dragon...]")

                val dragonsToSimulate = args.mapNotNull { arg ->
                    WitherDragonEnum.entries.find { it.name.equals(arg, ignoreCase = true) }
                }.takeUnless { it.isEmpty() } ?: return modMessage("No valid dragons specified.")

                // @formatter:off
                dragonsToSimulate.forEach { dragon ->
                    val x: Float
                    val z: Float

                    when (dragon) {
                        WitherDragonEnum.Red ->    { x = 27f; z = 60f }
                        WitherDragonEnum.Orange -> { x = 84f; z = 56f }
                        WitherDragonEnum.Green ->  { x = 26f; z = 95f }
                        WitherDragonEnum.Blue ->   { x = 84f; z = 95f }
                        WitherDragonEnum.Purple -> { x = 57f; z = 12f }
                        WitherDragonEnum.None -> return@forEach
                    }

                    DragonCheck.handleSpawnPacket(S2APacketParticles(EnumParticleTypes.FLAME, true, x, 19f, z, 2f, 3f, 2f, 0f, 20, 1))
                }

                val dragonNames = dragonsToSimulate.joinToString(", ") { it.name }
                modMessage("Simulating spawn logic for: $dragonNames")
            }

            "resetdrags" -> {
                event.isCanceled = true
                WitherDragonEnum.reset()
                modMessage("Dragons have been reset.")
            }

            "checkdrags" -> {
                event.isCanceled = true
                WitherDragonEnum.Red.state = WitherDragonEnum.Companion.WitherDragonState.ALIVE

                val (spawned, dragons) = WitherDragonEnum.entries.fold(0 to mutableListOf<WitherDragonEnum>()) { (spawned, dragons), dragon ->
                    val newSpawned = spawned + dragon.timesSpawned

                    if (dragon.state != WitherDragonEnum.Companion.WitherDragonState.DEAD) {
                        if (dragon !in dragons) {
                            modMessage("§${dragon.colorCode}${dragon.name} has been added to spawning list!")
                            dragons.add(dragon)
                        }
                        else modMessage("§${dragon.colorCode}${dragon.name} his already in spawning list!!")
                        return@fold newSpawned to dragons
                    }

                    modMessage("${dragons.size} is the current size of dragons")
                    modMessage("$newSpawned is how many dragons have spawned in total")
                    modMessage("&${dragon.colorCode}${dragon.name} has been skipped, since it isnt spawning!!")

                    newSpawned to dragons
                }

                if (dragons.size == 2 || spawned > 2) {
                    priorityDragon = findPriority(dragons)
                    if (priorityDragon.state != WitherDragonEnum.Companion.WitherDragonState.SPAWNING) priorityDragon = dragons.first { it != priorityDragon }
                    displaySpawningDragon(priorityDragon)
                    return modMessage("shouldve worked!")
                }
            }

            "endsplit" -> {
                dragonSpawnCount = 2
            }
        }
    }
}