package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.GsonUtils.gsonArray
import com.google.gson.JsonElement

class RangeSliderSetting<T: Number>(
    name: String,
    defaultValue: Pair<T, T>,
    val min: T,
    val max: T,
    val step: T,
    val suffix: String = ""
): ConfigHolder<Pair<T, T>>(name, defaultValue), Savable {
    private val scale = SliderSetting(name, min, min, max, step, suffix)

    val start get() = value.first
    val end get() = value.second

    override fun write() = gsonArray {
        add(value.first)
        add(value.second)
    }

    override fun read(element: JsonElement) {
        val array = element.asJsonArray
        value = clampRange(snapToStep(array[0].asDouble), snapToStep(array[1].asDouble))
    }

    fun clampRange(start: T, end: T): Pair<T, T> {
        if (start.toDouble() > end.toDouble()) return end to start
        return start to end
    }

    operator fun contains(number: Number) = number.toDouble() in start.toDouble() .. end.toDouble()

    fun snapToStep(rawDouble: Double) = scale.snapToStep(rawDouble)
    fun stringfy(v: T) = scale.stringfy(v)
    fun getPercent(valueIn: T) = scale.getPercent(valueIn)
}
