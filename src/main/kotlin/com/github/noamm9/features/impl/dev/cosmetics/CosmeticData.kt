package com.github.noamm9.features.impl.dev.cosmetics

import kotlinx.serialization.Serializable

@Serializable
data class CosmeticData(
    val name: String = "",
    val sizeX: Float = 1f,
    val sizeY: Float = 1f,
    val sizeZ: Float = 1f,
    val halo: Int? = null
) {
    val hasCustomName: Boolean get() = name.isNotEmpty()
    val hasCustomSize: Boolean get() = sizeX != 1f || sizeY != 1f || sizeZ != 1f
    val hasHalo: Boolean get() = halo != null
}