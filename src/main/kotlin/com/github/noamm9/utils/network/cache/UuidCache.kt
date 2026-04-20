package com.github.noamm9.utils.network.cache

import com.github.noamm9.config.PogObject
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.Utils.remove
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object UuidCache {
    private data class CacheData(
        val uuidByName: ConcurrentHashMap<String, CachedEntry> = ConcurrentHashMap(),
        val nameByUuid: ConcurrentHashMap<String, CachedEntry> = ConcurrentHashMap()
    )

    private val storage = PogObject("uuid_cache", CacheData())
    private val EXPIRE_TIME = TimeUnit.HOURS.toMillis(1)
    private val nameRegex = "^\\w+$".toRegex()

    init {
        ThreadUtils.loop(TimeUnit.MINUTES.toMillis(10), block = ::cleanupExpired)

        EventBus.register<MainThreadPacketReceivedEvent.Post> {
            val packet = event.packet as? ClientboundPlayerInfoUpdatePacket ?: return@register
            if (! packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) return@register

            for (entry in packet.entries()) {
                val profile = entry.profile() ?: continue
                val name = profile.name.takeIf { it.length in 3 .. 16 && nameRegex.matches(it) } ?: continue
                val uuid = profile.id.takeIf { it.version() == 4 } ?: continue
                addToCache(name, uuid.toString())
            }
        }
    }

    fun addToCache(name: String, uuid: String) {
        val data = storage.getData()
        val cleanUuid = uuid.remove("-")

        if (cleanUuid == "FAILED") data.uuidByName[name.lowercase()] = CachedEntry("FAILED", System.currentTimeMillis())
        else if (name == "FAILED") data.nameByUuid[cleanUuid] = CachedEntry("FAILED", System.currentTimeMillis())
        else {
            data.uuidByName[name.lowercase()] = CachedEntry(cleanUuid, System.currentTimeMillis())
            data.nameByUuid[cleanUuid] = CachedEntry(name, System.currentTimeMillis())
        }
    }

    fun getFromCache(name: String): String? {
        val lowerName = name.lowercase()
        val data = storage.getData()
        val entry = data.uuidByName[lowerName] ?: return null

        if (entry.isExpired()) {
            data.uuidByName.remove(lowerName)
            data.nameByUuid.remove(entry.value)
            return null
        }

        return entry.value
    }

    fun getNameFromCache(uuid: String): String? {
        val cleanUuid = uuid.remove("-")
        val data = storage.getData()
        val entry = data.nameByUuid[cleanUuid] ?: return null

        if (entry.isExpired()) {
            data.nameByUuid.remove(cleanUuid)
            data.uuidByName.remove(entry.value.lowercase())
            return null
        }

        return entry.value
    }

    private fun cleanupExpired() {
        val data = storage.getData()
        val now = System.currentTimeMillis()
        val removedUuids = data.uuidByName.values.removeIf { it.isExpired(now) }
        val removedNames = data.nameByUuid.values.removeIf { it.isExpired(now) }
        if (removedUuids || removedNames) storage.save()
    }

    private fun CachedEntry.isExpired(now: Long = System.currentTimeMillis()) = now - timestamp > EXPIRE_TIME
    private data class CachedEntry(val value: String, val timestamp: Long)
}