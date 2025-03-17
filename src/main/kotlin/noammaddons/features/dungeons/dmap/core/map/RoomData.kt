package noammaddons.features.dungeons.dmap.core.map

data class RoomData(
    val name: String,
    val type: RoomType,
    val cores: List<Int>,
    val crypts: Int = 0,
    val secrets: Int = 0,
    val trappedChests: Int = 0,
) {
    companion object {
        fun createUnknown(type: RoomType) = RoomData("Unknown", type, emptyList(), 0, 0, 0)
    }
}
