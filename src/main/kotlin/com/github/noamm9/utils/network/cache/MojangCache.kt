package com.github.noamm9.utils.network.cache

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.config.PogObject
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.network.abstracts.CacheResult
import com.github.noamm9.utils.network.abstracts.CachedEntry
import com.github.noamm9.utils.network.abstracts.NetworkCache
import com.github.noamm9.utils.network.data.MojangData
import com.github.noamm9.utils.remove
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import java.util.concurrent.*

object MojangCache: NetworkCache<String, MojangData> {
    private var storage = PogObject("mojang_cache", ConcurrentHashMap<String, CachedEntry<MojangData>>())
    private val EXPIRE_TIME = TimeUnit.HOURS.toMillis(1)
    private val nameRegex = "^\\w+$".toRegex()

    init {
        ThreadUtils.loop(TimeUnit.MINUTES.toMillis(10), block = ::cleanupExpired)
        addToCache(MojangData(mc.user.name, mc.user.profileId.toString().remove("-")))

        EventBus.register<MainThreadPacketReceivedEvent.Post> {
            val packet = event.packet as? ClientboundPlayerInfoUpdatePacket ?: return@register
            if (! packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) return@register

            for (entry in packet.entries()) {
                val profile = entry.profile() ?: continue
                val name = profile.name.takeIf { it.length in 3 .. 16 && nameRegex.matches(it) } ?: continue
                val uuid = profile.id.takeIf { it.version() == 4 } ?: continue
                addToCache(MojangData(name, uuid.toString().remove("-")))
                ChatUtils.debug("uc", "added $name to cache from packet")
            }
        }
    }

    override fun get(key: String): CacheResult<MojangData> {
        val cleanKey = key.remove("-").lowercase()
        val entry = storage.get()[cleanKey] ?: return CacheResult.NotFound

        if (System.currentTimeMillis() - entry.timestamp > EXPIRE_TIME) {
            storage.get().remove(cleanKey)
            entry.value?.let { data ->
                storage.get().remove(data.name.lowercase())
                storage.get().remove(data.uuid.remove("-").lowercase())
            }
            return CacheResult.NotFound
        }

        return if (entry.value == null) CacheResult.Failed else CacheResult.Success(entry.value)
    }

    fun addToCache(data: MojangData) = addToCache("", data)
    override fun addToCache(key: String, value: MojangData) {
        val cleanUuid = value.uuid.remove("-").lowercase()
        val lowerName = value.name.lowercase()
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