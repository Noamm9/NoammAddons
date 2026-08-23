package com.github.noamm9.config.types

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.Savable
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import kotlin.math.round

class SliderSetting<T: Number>(
    name: String,
    defaultValue: T,
    val min: T,
    val max: T,
    val step: T,
    val suffix: String = ""
): ConfigHolder<T>(name, defaultValue), Savable {
    override fun write() = JsonPrimitive(value)
    override fun read(element: JsonElement) {
        value = snapToStep(element.asDouble)
    }

    fun getPercent(valueIn: T): Float {
        val current = valueIn.toDouble()
        val minD = min.toDouble()
        val maxD = max.toDouble()
        if (maxD - minD == 0.0) return 0f
        return ((current - minD) / (maxD - minD)).toFloat()
    }

    fun snapToStep(rawDouble: Double): T {
        val minD = min.toDouble()
        val maxD = max.toDouble()
        val stepD = step.toDouble()
        val clamped = rawDouble.coerceIn(minD, maxD)

        if (stepD <= 0) return clamped.convertToType()

        val steps = round((clamped - minD) / stepD)
        val steppedValue = (minD + (steps * stepD)).coerceIn(minD, maxD)

        return steppedValue.convertToType()
    }

    fun stringfy(v: T): String {
        return when (v) {
            is Int, is Long -> v.toLong().toString()
            else -> {
                val dVal = v.toDouble()
                val stepD = step.toDouble()
                if (stepD % 1.0 == 0.0) dVal.toFixed(0)
                else dVal.toFixed(2)
            }
        }
    }

    private fun Number.convertToType(): T {
        @Suppress("UNCHECKED_CAST")
        return when (min) {
            is Int -> toInt() as T
            is Long -> toLong() as T
            is Float -> toFloat() as T
            is Double -> toDouble() as T
            else -> this as T
        }
    }
}