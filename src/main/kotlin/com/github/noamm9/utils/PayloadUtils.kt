package com.github.noamm9.utils

import com.github.noamm9.NoammAddons
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

/**
 * Rebuilds a `minecraft:register` / `minecraft:unregister` payload so it only advertises
 * the channels we are willing to expose.
 *
 * The concrete payload type is a Fabric implementation detail, so the replacement has to be
 * built reflectively off whatever constructor that class happens to expose.
 */
object PayloadUtils {
    fun filterRegisterPayload(original: CustomPacketPayload, allowedChannels: Set<String>): CustomPacketPayload? {
        val ids = allowedChannels.mapNotNull { Identifier.tryParse(it) }

        runCatching {
            for (constructor in original.javaClass.declaredConstructors) {
                constructor.isAccessible = true
                val params = constructor.parameterTypes

                when (params.size) {
                    1 -> when {
                        List::class.java.isAssignableFrom(params[0]) ->
                            return constructor.newInstance(ids) as CustomPacketPayload

                        Set::class.java.isAssignableFrom(params[0]) ->
                            return constructor.newInstance(ids.toSet()) as CustomPacketPayload

                        Collection::class.java.isAssignableFrom(params[0]) ->
                            return constructor.newInstance(ids) as CustomPacketPayload
                    }

                    2 -> if (
                        CustomPacketPayload.Type::class.java.isAssignableFrom(params[0]) &&
                        Collection::class.java.isAssignableFrom(params[1])
                    ) return constructor.newInstance(original.type(), ids) as CustomPacketPayload
                }
            }
        }.onFailure { NoammAddons.logger.error("[ModHider] Failed to rebuild register payload", it) }

        return null
    }
}
