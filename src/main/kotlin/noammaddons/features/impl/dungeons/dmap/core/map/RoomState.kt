package noammaddons.features.impl.dungeons.dmap.core.map

/**
 * [ordinal] matters here, should be in the order of what can happen to a room.
 */
enum class RoomState {
    FAILED, GREEN, CLEARED, DISCOVERED, UNOPENED, UNDISCOVERED
}
