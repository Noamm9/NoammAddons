package noammaddons.ui.config.core.save

import com.google.gson.JsonElement

internal interface Savable {
    fun write(): JsonElement
    fun read(element: JsonElement?)
}