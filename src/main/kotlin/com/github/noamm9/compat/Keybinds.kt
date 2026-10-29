package com.github.noamm9.compat

import com.google.gson.JsonObject

//~ if <26.3 '() = addProperty("inputSystem", "SDL")' -> '() {}'
fun JsonObject.writeInputSystem() = addProperty("inputSystem", "SDL")

//~ if <26.3 '= obj.get("inputSystem")?.asString != "SDL"' -> '= false'
fun isLegacyKeybind(obj: JsonObject) = obj.get("inputSystem")?.asString != "SDL"

//~ if <26.3 '= LegacyKeybinds.from(key, isMouse)' -> '= key'
fun migrateLegacyKey(key: Int, isMouse: Boolean) = LegacyKeybinds.from(key, isMouse)
