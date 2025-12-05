package noammaddons.features.impl.dungeons.dmap.core.map

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
        val redstoneKey: List<Map<String, Int>> = emptyList(),
        val wither: List<Map<String, Int>> = emptyList(),
        val bat: List<Map<String, Int>> = emptyList(),
        val item: List<Map<String, Int>> = emptyList(),
        val chest: List<Map<String, Int>> = emptyList(),
    )

    companion object {
        fun createUnknown(type: RoomType) = RoomData("Unknown", type, "", emptyList(), SecretDetails(), SecretCoords())
    }
}