package noammaddons.features.dungeons.dmap.core.map

enum class RoomType {
    BLOOD, CHAMPION, ENTRANCE, FAIRY, NORMAL, PUZZLE, RARE, TRAP;

    companion object {
        fun fromMapColor(color: Int): RoomType? = when (color) {
            18 -> BLOOD
            74 -> CHAMPION
            30 -> ENTRANCE
            82 -> FAIRY
            63, 85 -> NORMAL
            66 -> PUZZLE
            62 -> TRAP
            else -> null
        }
    }
}
