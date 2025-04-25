package noammaddons.features.impl.dungeons.dmap.core.map

enum class DoorType {
    BLOOD, ENTRANCE, NORMAL, WITHER;

    companion object {
        fun fromMapColor(color: Int): DoorType? = when (color) {
            18 -> BLOOD
            30 -> ENTRANCE
            // Champion, Fairy, Puzzle, Trap, Unopened doors render as normal doors
            74, 82, 66, 62, 85, 63 -> NORMAL
            119 -> WITHER
            else -> null
        }
    }
}
