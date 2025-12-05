package noammaddons.features.impl.dungeons.dmap.core.map

import net.minecraft.util.BlockPos

data class RoomData(
    val name: String,
    val type: RoomType,
    val shape: String,
    val cores: List<Int>,
    val secretDetails: SecretDetails,
    val secretCoords: SecretCoords,
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

    companion object {
        fun createUnknown(type: RoomType) = RoomData("Unknown", type, "", emptyList(), SecretDetails(), SecretCoords())
    }
}