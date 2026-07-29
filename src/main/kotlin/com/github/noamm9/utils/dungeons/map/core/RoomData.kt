package com.github.noamm9.utils.dungeons.map.core

import net.minecraft.core.BlockPos

data class RoomData(
    val name: String,
    val type: RoomType,
    val shape: RoomShape,
    val cores: List<Int>,
    val secretDetails: SecretDetails,
    val secretCoords: SecretCoords,
    val trappedChests: Int = 0,
    val reviveStones: Int = 0,
    val secrets: Int = 0,
    val crypts: Int = 0,
) {
    data class SecretDetails(
        val redstoneKey: Int = 0,
        val wither: Int = 0,
        val bat: Int = 0,
        val item: Int = 0,
        val chest: Int = 0
    )

    data class SecretCoords(
        val redstoneKey: List<BlockPos> = emptyList(),
        val wither: List<BlockPos> = emptyList(),
        val bat: List<BlockPos> = emptyList(),
        val item: List<BlockPos> = emptyList(),
        val chest: List<BlockPos> = emptyList(),
    )

    fun isUnknown() = name == "Unknown" && shape == RoomShape.UNKNOWN

    companion object {
        fun createUnknown(type: RoomType) = RoomData("Unknown", type, RoomShape.UNKNOWN, emptyList(), SecretDetails(), SecretCoords())
    }
}