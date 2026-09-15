package com.github.noamm9.utils.network.cache

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.config.PogObject
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.utils.*
import com.github.noamm9.utils.network.abstracts.*
import com.github.noamm9.utils.network.data.MojangData
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import java.util.concurrent.*

object MojangCache: NetworkCache<String, MojangData> {
    private val storage = PogObject("mojang_cache", ConcurrentHashMap<String, CachedEntry<MojangData>>())
    private val EXPIRE_TIME = TimeUnit.HOURS.toMillis(1)
    private val nameRegex = "^\\w{3,16}$".toRegex()

    init {
        ThreadUtils.loop(TimeUnit.MINUTES.toMillis(10), block = ::cleanupExpired)
        addToCache(MojangData(mc.user.name, mc.user.profileId.toString()))

        EventBus.register<MainThreadPacketReceivedEvent.Post> {
            val packet = event.packet as? ClientboundPlayerInfoUpdatePacket ?: return@register
            if (! packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) return@register

            for (entry in packet.entries()) {
                val profile = entry.profile() ?: continue
                val name = profile.name.takeIf { nameRegex.matches(it) } ?: continue
                val uuid = profile.id.takeIf { it.version() == 4 } ?: continue
                addToCache(MojangData(name, uuid.toString()))
                ChatUtils.debug("mojang-cache", "added $name to cache from packet")
            }
        }
    }

    override fun get(key: String): CacheResult<MojangData> {
        val cleanKey = key.remove("-").lowercase()
        val entry = storage.get()[cleanKey] ?: return CacheResult.NotFound

        if (System.currentTimeMillis() - entry.timestamp > EXPIRE_TIME) {
            entry.value?.let { data ->
                storage.get().remove(data.name.lowercase())
                storage.get().remove(data.uuid.remove("-").lowercase())
            } ?: storage.get().remove(cleanKey)
            return CacheResult.NotFound
        }

        return if (entry.value == null) CacheResult.Failed else CacheResult.Success(entry.value)
    }

    fun addToCache(data: MojangData) = addToCache("", data)
    override fun addToCache(key: String, value: MojangData) {
        val cleanUuid = value.uuid.remove("-").lowercase()
        val lowerName = value.name.lowercase()

        storage.get()[cleanUuid]?.value?.let { old ->
            val oldNameKey = old.name.lowercase()
            if (oldNameKey != lowerName) storage.get().remove(oldNameKey)
        }

        val entry = CachedEntry(value)
        storage.get()[lowerName] = entry
        storage.get()[cleanUuid] = entry
    }

    override fun addFailedToCache(key: String) = storage.get().set(key.remove("-").lowercase(), CachedEntry(null))

    private fun cleanupExpired() {
        val now = System.currentTimeMillis()
        val removed = storage.get().values.removeIf { now - it.timestamp > EXPIRE_TIME }
        if (removed) storage.save()
    }
}