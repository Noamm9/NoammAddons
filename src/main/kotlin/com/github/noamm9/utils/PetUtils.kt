package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.MOD_ID
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.config.PogObject
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.init.types.ISelfInit
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.items.ItemRarity
import com.github.noamm9.utils.items.ItemUtils.customData
import com.github.noamm9.utils.items.ItemUtils.getSkullTexture
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.location.LocationUtils
import com.google.common.collect.ImmutableMultimap
import com.google.gson.JsonObject
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import java.util.*
import java.util.concurrent.*
import kotlin.jvm.optionals.getOrNull

object PetUtils: ISelfInit {
    private val summonPattern = Regex("""^§aYou (summoned|despawned) your (.+?)(?: ✦)?§a!$""")
    private val autopetPattern = Regex("""^§cAutopet §eequipped your §7\[Lvl (\d+)] (.+?)§e! §a§lVIEW RULE$""")
    private val levelUpPattern = Regex("^Your (.+?) leveled up to level (\\d+)!$")
    private val petMenuPattern = Regex("""^(?:\(\d+/\d+\) )?Pets(?: \(\d+/\d+\))?$""")
    private val petItemNamePattern = Regex("""^(?:(?:§[0-9a-fk-or])?⭐\s*)?(?:§[0-9a-f])?\[Lvl\s+(\d+)](?: §8\[.*?])?\s+(.+)""")
    private val colorCodePattern = Regex("§[0-9a-f]")
    private val loadoutsPetRegex = Regex("\\[Lvl (\\d+)] (.+)$")
    private val loadoutSlots = setOf(14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43)

    private val petCache = PogObject("petCache", PetCache())
    private val knownPets get() = petCache.get().known
    private var clickedPet: Pet? = null

    private val headCache = ConcurrentHashMap<String, ItemStack>()
    private val steveHead by lazy { ItemStack(Items.PLAYER_HEAD) }

    var currentPet: Pet?
        private set(value) = petCache.get()::active.set(value).also { petCache.save() }
        get() = petCache.get().active

    override fun init() {
        EventBus.register<ChatMessageEvent> {
            if (! LocationUtils.inSkyblock) return@register

            summonPattern.matchEntire(event.formattedText)?.destructured?.let { (action, name) ->
                val pet = if (action == "summoned") {
                    val rarity = event.component.petRarity(name)
                    clickedPet
                        ?.takeIf { it.matches(name) }
                        ?.merge(Pet(name, rarity = rarity))
                        ?: resolvePet(name, rarity = rarity, skin = clickedPet?.skin)
                }
                else null

                clickedPet = null
                changePet(pet, if (pet == null) PetEvent.Cause.DESPAWN else PetEvent.Cause.SUMMON)
                return@register
            }

            autopetPattern.matchEntire(event.formattedText)?.destructured?.let { (level, name) ->
                clickedPet = null
                changePet(resolvePet(name, level.toIntOrNull(), event.component.petRarity(name)), PetEvent.Cause.AUTOPET)
                return@register
            }

            levelUpPattern.matchEntire(event.unformattedText)?.destructured?.let { (name, level) ->
                levelUpPet(name, level.toIntOrNull() ?: return@let)
            }
        }

        EventBus.register<ContainerFullyOpenedEvent> {
            if (! LocationUtils.inSkyblock) return@register
            if (! petMenuPattern.matches(event.title.unformattedText)) return@register
            ThreadUtils.async {
                var equipped: Pet? = null
                var found = 0

                for (stack in event.items.values) {
                    val pet = stack.pet() ?: continue
                    cachePet(pet)
                    found ++

                    if (equipped == null && stack.lore.any { it.removeFormatting().contains("Click to despawn!", ignoreCase = true) }) {
                        equipped = pet
                    }
                }

                ChatUtils.debug("pet", "scanned $found pets from menu")

                equipped?.let {
                    if (equipped.uuid == null) return@let
                    if (clickedPet?.uuid == equipped.uuid) return@let

                    val current = currentPet

                    when {
                        current == null -> changePet(equipped, PetEvent.Cause.MENU)
                        current.uuid == null && current.matches(equipped.formattedName) -> updatePet(current.merge(equipped))
                        current.uuid != equipped.uuid -> changePet(equipped, PetEvent.Cause.MENU)
                        else -> updatePet(current.merge(equipped))
                    }
                }

                petCache.save()
            }
        }

        EventBus.register<ContainerEvent.SlotClick> {
            if (event.button != 0) return@register
            if (event.clickType != ContainerInput.PICKUP) return@register
            if (event.slotId !in 10 .. 43 || event.slotId % 9 !in 1 .. 7) return@register
            if (! petMenuPattern.matches(event.screen.title.unformattedText)) return@register

            clickedPet = mc.player?.containerMenu?.items?.getOrNull(event.slotId)?.pet()
        }

        EventBus.register<ContainerEvent.SlotClick> {
            if (event.button != 0) return@register
            if (event.clickType != ContainerInput.PICKUP) return@register
            if (event.slotId !in loadoutSlots) return@register
            if (! event.screen.title.unformattedText.endsWith(") Loadouts")) return@register
            mc.player?.containerMenu?.items[event.slotId]?.lore?.find { it.startsWith("§7Pet: ") }?.let {
                val match = loadoutsPetRegex.find(it)?.destructured ?: return@let
                val level = match.component1()
                val name = match.component2()
                clickedPet = null
                changePet(resolvePet(name, level.toIntOrNull(), ItemRarity.byBaseColor(name.take(2))), PetEvent.Cause.MENU)
            }
        }

        EventBus.register<WorldChangeEvent> { clickedPet = null }
    }

    fun ItemStack.pet(): Pet? {
        if (isEmpty) return null

        val tag = customData
        val rawPetInfo = tag.getString("petInfo").getOrNull()?.takeIf(String::isNotEmpty) ?: return null
        val petInfo = catch { GsonUtils.gson.fromJson(rawPetInfo, JsonObject::class.java) } ?: return null

        val rarity = ItemRarity.fromName(catch { petInfo.get("tier").asString }).takeUnless { it == ItemRarity.NONE }
        val displayMatch = petItemNamePattern.matchEntire(hoverName.formattedText)

        var formattedName = displayMatch?.groupValues?.getOrNull(2)?.trim()?.takeIf(String::isNotEmpty)
        if (formattedName == null) formattedName = run {
            val color = rarity?.baseColor?.toString() ?: return@run null
            val name = catch { petInfo.get("type").asString }?.replace('_', ' ')?.lowercase()?.split(' ')?.joinToString(" ") { it.uppercaseFirst() } ?: return@run null
            return@run "$color$name"
        }

        val heldItem = catch {
            val sbid = petInfo.get("heldItem").asString?.takeUnless { it.isBlank() || it == "null" || it == "NONE" }
            sbid?.lowercase()?.split("_")?.joinToString(" ") { it.uppercaseFirst() }
        }

        return Pet(
            formattedName = formattedName ?: return null,
            level = displayMatch?.groupValues?.getOrNull(1)?.toIntOrNull(),
            rarity = rarity,
            uuid = tag.getString("uuid").getOrNull() ?: catch { petInfo.get("uuid").asString },
            heldItem = heldItem,
            skin = getSkullTexture(this),
        )
    }

    private fun resolvePet(name: String, level: Int? = null, rarity: ItemRarity? = null, skin: String? = null): Pet {
        val base = knownPets.values.singleOrNull { it.matches(name, rarity) }

        return Pet(
            formattedName = base?.formattedName ?: name,
            level = level ?: base?.level,
            rarity = rarity ?: base?.rarity,
            uuid = base?.uuid,
            heldItem = base?.heldItem,
            skin = base?.skin ?: skin
        )
    }

    private fun changePet(pet: Pet?, cause: PetEvent.Cause) {
        currentPet = pet
        pet?.let(::cachePet)
        EventBus.post(PetEvent.Change(pet, cause))
    }

    private fun updatePet(pet: Pet) {
        if (currentPet == pet) return

        currentPet = pet
        cachePet(pet)
    }

    private fun levelUpPet(name: String, level: Int) {
        val current = currentPet?.takeIf { it.matches(name) } ?: return
        if (current.level == level) return

        val pet = current.copy(level = level)
        updatePet(pet)
        EventBus.post(PetEvent.LevelUp(pet))
    }

    private fun cachePet(pet: Pet) {
        val uuid = pet.uuid

        if (uuid == null) {
            ChatUtils.debug("pet", "not caching ${pet.formattedName}, missing uuid")
            return
        }

        knownPets[uuid] = knownPets[uuid]?.merge(pet) ?: pet
        petCache.save()
    }

    private fun Pet.merge(other: Pet) = Pet(
        formattedName = other.formattedName,
        level = other.level ?: level,
        rarity = other.rarity ?: rarity,
        uuid = other.uuid ?: uuid,
        heldItem = other.heldItem ?: heldItem,
        skin = other.skin ?: skin,
    )

    private fun Component.petRarity(name: String): ItemRarity? {
        val formatted = formattedText
        val plain = Pet.cleanName(name)
        val index = formatted.indexOf(plain, ignoreCase = true)
        if (index < 0) return null

        val color = colorCodePattern.findAll(formatted.substring(0, index)).lastOrNull()?.value ?: return null

        return ItemRarity.byBaseColor(color)
    }

    private data class PetCache(var active: Pet? = null, val known: ConcurrentHashMap<String, Pet> = ConcurrentHashMap())

    data class Pet(
        val formattedName: String,
        val level: Int? = null,
        val rarity: ItemRarity? = null,
        val uuid: String? = null,
        val heldItem: String? = null,
        val skin: String? = null
    ) {
        fun matches(name: String, rarity: ItemRarity? = null) = normalizeName(formattedName) == normalizeName(name) && rarity.equalsOneOf(null, this.rarity)

        fun head(): ItemStack {
            val skin = skin ?: return steveHead

            return headCache.getOrPut(skin) {
                ItemStack(Items.PLAYER_HEAD).apply {
                    val properties = PropertyMap(ImmutableMultimap.of("textures", Property("textures", skin)))
                    val profile = GameProfile(UUID.nameUUIDFromBytes("$MOD_ID:$formattedName".toByteArray()), this::class.simpleName, properties)
                    set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile))
                }
            }
        }

        companion object {
            private val levelRegex = Regex("""^\[Lvl\s+\d+]\s*""")

            fun cleanName(name: String) = name
                .removeFormatting()
                .remove("⭐", "✦")
                .remove(levelRegex)
                .trim()

            fun normalizeName(name: String) = cleanName(name).lowercase()
        }
    }
}