package com.github.noamm9.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import net.minecraft.core.BlockPos
import java.awt.Color

object JsonUtils {
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        serializersModule = SerializersModule {
            contextual(Color::class, ColorSerializer)
            contextual(BlockPos::class, BlockPosSerializer)
        }
    }

    fun JsonObject.getObj(key: String) = this[key]?.jsonObject
    fun JsonObject.getString(key: String) = this[key]?.jsonPrimitive?.content


    object ColorSerializer: KSerializer<Color> {
        override val descriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.INT)
        override fun serialize(encoder: Encoder, value: Color) = encoder.encodeInt(value.rgb)
        override fun deserialize(decoder: Decoder) = Color(decoder.decodeInt(), true)
    }

    object BlockPosSerializer: KSerializer<BlockPos> {
        override val descriptor = buildClassSerialDescriptor("BlockPos") {
            element<Int>("x")
            element<Int>("y")
            element<Int>("z")
        }

        override fun serialize(encoder: Encoder, value: BlockPos) = encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.x)
            encodeIntElement(descriptor, 1, value.y)
            encodeIntElement(descriptor, 2, value.z)
        }

        override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
            BlockPos.MutableBlockPos().apply {
                while (true) when (val index = decodeElementIndex(descriptor)) {
                    0 -> x = decodeIntElement(descriptor, 0)
                    1 -> y = decodeIntElement(descriptor, 1)
                    2 -> z = decodeIntElement(descriptor, 2)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index $index")
                }
            }
        }
    }
}