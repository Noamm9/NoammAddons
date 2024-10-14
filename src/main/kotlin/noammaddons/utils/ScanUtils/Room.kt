package noammaddons.utils.ScanUtils


data class Room(
	val id: List<String>?,
	val name: String,
	val type: String,
	val shape: String,
	val doors: String?,
	val secrets: Int,
	val crypts: Int,
	val revive_stones: Int,
	val journals: Int,
	val spiders: Boolean,
	val secret_details: SecretDetails,
	val soul: Boolean,
	val cores: List<Int>,
	val secret_coords: SecretCoords?
)

data class SecretDetails(
	val wither: Int,
	val redstone_key: Int,
	val bat: Int,
	val item: Int,
	val chest: Int
)

data class SecretCoords(
	val chest: List<List<Int>>?,
	val item: List<List<Int>>?,
	val bat: List<List<Int>>?
)


data class Coords2D(
	val x: Int,
	val z: Int
)

