package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.config.PogObject
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.event.priority.EventPriority
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.PetUtils.Pet
import com.github.noamm9.utils.PetUtils.pet
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.pets.LoadoutPetSync
import net.minecraft.network.protocol.game.*
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import java.util.concurrent.ConcurrentHashMap

object LoadoutPets {
    private val cache = PogObject("loadoutPets", ConcurrentHashMap<String, Pet>())
    private val menuPattern = Regex("""^\((\d+)/\d+\) Loadouts$""")
    private val petsPattern = Regex("""^(?:\(\d+/\d+\) )?Pets(?: \(\d+/\d+\))?$""")
    private val equippedPattern = Regex("^You equipped (.+)!$")
    private val slots = listOf(14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43)
    private const val petSlot = 21 // Row 3, column 4, in both the overview and editor.

    private enum class Kind { OVERVIEW, EDITOR, PET_SELECTION }
    private data class Menu(val id: Int, val kind: Kind, val page: Int = 0, val key: String? = null)
    private var menu: Menu? = null
    private val items = mutableMapOf<Int, ItemStack>()
    private var editorKey: String? = null
    private var editorTitle: String? = null
    private var enteringEditor = false
    private var enteringPets = false
    private var closedUntil = Long.MAX_VALUE
    private lateinit var sync: LoadoutPetSync<Pet>

    fun init(display: (Pet) -> Unit) {
        sync = LoadoutPetSync({ cache.get()[it] }, ::save, display)
        EventBus.register<MainThreadPacketReceivedEvent.Pre> {
            when (val packet = event.packet) {
                is ClientboundOpenScreenPacket -> open(packet.containerId, packet.title.unformattedText)
                is ClientboundContainerSetSlotPacket -> receive(packet.containerId, packet.slot, packet.item)
                is ClientboundContainerSetContentPacket -> packet.items.forEachIndexed { slot, item -> receive(packet.containerId, slot, item) }
                is ClientboundContainerClosePacket -> closed(packet.containerId)
            }
        }
        EventBus.register<ChatMessageEvent>(receiveCancelled = true) {
            if (!LocationUtils.inSkyblock) return@register
            equippedPattern.matchEntire(event.unformattedText)?.groupValues?.get(1)?.let(sync::confirm)
        }
        // Observe actual outgoing clicks so keyboard loadout binds work too.
        EventBus.register<PacketEvent.Sent>(EventPriority.LOWEST) {
            when (val packet = event.packet) {
                is ServerboundContainerClickPacket -> mc.execute {
                    click(packet.containerId, packet.slotNum.toInt(), packet.buttonNum.toInt(), packet.containerInput)
                }
                is ServerboundContainerClosePacket -> mc.execute { closed(packet.containerId) }
            }
        }
        EventBus.register<WorldChangeEvent> {
            menu = null
            items.clear()
            resetEditor()
            sync.reset()
        }
    }

    private fun open(id: Int, title: String) {
        val page = menuPattern.matchEntire(title)?.groupValues?.get(1)?.toIntOrNull()
        menu = when {
            page != null -> {
                resetEditor()
                Menu(id, Kind.OVERVIEW, page)
            }
            enteringEditor -> {
                enteringEditor = false
                editorTitle = title
                Menu(id, Kind.EDITOR, key = editorKey)
            }
            editorKey != null && title == editorTitle -> {
                enteringPets = false
                Menu(id, Kind.EDITOR, key = editorKey)
            }
            editorKey != null && (enteringPets || menu?.kind == Kind.PET_SELECTION) && petsPattern.matches(title) ->
                Menu(id, Kind.PET_SELECTION, key = editorKey)
            else -> {
                resetEditor()
                sync.reset()
                null
            }
        }
        items.clear()
        closedUntil = Long.MAX_VALUE
    }

    private fun receive(id: Int, slot: Int, stack: ItemStack) {
        val current = menu?.takeIf { it.id == id } ?: return
        if (System.currentTimeMillis() > closedUntil) return
        if (slot !in 0 until 54) return
        items[slot] = stack.copy()
        if (slot != petSlot) return
        val pet = stack.pet() ?: return
        when (current.kind) {
            // This is the actual equipped pet, even if it arrives before chat or after closing.
            Kind.OVERVIEW -> sync.receive(pet)
            // The editor reports the assignment, not necessarily the currently equipped pet.
            Kind.EDITOR -> current.key?.let { save(it, pet) }
            Kind.PET_SELECTION -> Unit
        }
    }

    private fun click(id: Int, slot: Int, button: Int, input: ContainerInput) {
        if (!LocationUtils.inSkyblock || input != ContainerInput.PICKUP) return
        val current = menu?.takeIf { it.id == id } ?: return
        val item = items[slot] ?: return
        when (current.kind) {
            Kind.OVERVIEW -> {
                val index = slots.indexOf(slot).takeIf { it >= 0 } ?: return
                val key = "${mc.user.profileId}:${current.page}:$index"
                if (button == 1) {
                    editorKey = key
                    enteringEditor = true
                    sync.reset()
                }
                else if (button == 0 && item.lore.any { it.removeFormatting().contains("Left-click to equip!") }) {
                    sync.select(key, item.hoverName.unformattedText.trim())
                }
            }
            Kind.EDITOR -> if (slot == petSlot) {
                if (button == 0) enteringPets = true
                else if (button == 1) current.key?.let(::invalidate)
            }
            Kind.PET_SELECTION -> if (button == 0 && item.lore.any { it.removeFormatting() == "Click to select!" }) {
                val pet = item.pet() ?: return
                current.key?.let { save(it, pet) }
            }
        }
    }

    private fun closed(id: Int) {
        if (menu?.id != id) return
        // Allow the response in flight, but don't treat a later reused container as this menu.
        closedUntil = System.currentTimeMillis() + 5000L
    }

    private fun save(key: String, pet: Pet) {
        if (cache.get().put(key, pet) != pet) cache.save()
    }

    private fun invalidate(key: String) {
        if (cache.get().remove(key) != null) cache.save()
    }

    private fun resetEditor() {
        editorKey = null
        editorTitle = null
        enteringEditor = false
        enteringPets = false
    }
}
