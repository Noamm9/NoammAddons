package NoammAddons.utils

import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.LocationUtils.inDungeons
import NoammAddons.utils.TablistUtils.getTabList
import NoammAddons.utils.ChatUtils.removeFormatting
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.awt.Color

// Big Thanks to Odin for Having their code available.
// https://github.com/Noamm9/OdinClient/blob/main/odinmain/src/main/kotlin/me/odinmain/utils/skyblock/dungeon/DungeonUtils.kt

object DungeonUtils {
    /**
     * Enumeration representing player classes in a dungeon setting.
     *
     * Each class is associated with a specific code and color used for formatting in the game. The classes include
     * Archer, Mage, Berserk, Healer, and Tank.
     *
     * @property color The color associated with the class.
     * @property prio The priority of the class.
     */
    enum class Classes(val color: Color, var prio: Int, ) {
        Archer(Color(255, 0, 0), 0),
        Berserk(Color(255,106,0),1),
        Healer(Color(255, 0, 255), 1),
        Mage(Color(0, 255, 255), 1),
        Tank(Color(0, 255, 0), 0),
    }

    /**
     * Data class representing a player in a dungeon, including their name, class, skin location, and associated player entity.
     *
     * @property name The name of the player.
     * @property clazz The player's class, defined by the [Classes] enum.
     * @property locationSkin The resource location of the player's skin.
     * @property entity The optional associated player entity. Defaults to `null`.
     */
    data class DungeonPlayer(
        val name: String,
        var clazz: Classes,
        val locationSkin: ResourceLocation = ResourceLocation("textures/entity/steve.png"),
        val entity: EntityPlayer? = null,
        var isDead: Boolean = false
    )


    fun getDungeonTabList(): List<Pair<NetworkPlayerInfo, String>>? {
        val tabEntries = getTabList
        if (tabEntries.size < 18 || !tabEntries[0].second.contains("§r§b§lParty §r§f(")) return null
        return tabEntries
    }

    private fun addTeammate(name: String, clazz: String, teammates: MutableList<DungeonPlayer>, networkPlayerInfo: NetworkPlayerInfo) {
        Classes.entries.find { it.name == clazz }?.let { foundClass ->
            mc.theWorld.getPlayerEntityByName(name)?.let { player ->
                teammates.add(DungeonPlayer(name, foundClass, networkPlayerInfo.locationSkin, player))
            } ?: teammates.add(DungeonPlayer(name, foundClass, networkPlayerInfo.locationSkin, null))
        }
    }


    private val tablistRegex = Regex("^\\[(\\d+)] (?:\\[\\w+] )*(\\w+) .*?\\((\\w+)(?: (\\w+))*\\)$")

    private fun getDungeonTeammates(previousTeammates: List<DungeonPlayer>): List<DungeonPlayer> {
        val teammates = mutableListOf<DungeonPlayer>()
        val tabList = getDungeonTabList() ?: return emptyList()

        for ((networkPlayerInfo, line) in tabList) {

            val (_, sbLevel, name, clazz, clazzLevel) = tablistRegex.find(line.removeFormatting())?.groupValues ?: continue

            addTeammate(name, clazz, teammates, networkPlayerInfo) // will fail to find the EMPTY or DEAD class and won't add them to the list
            if (clazz == "DEAD" || clazz == "EMPTY") {
                val previousClass = previousTeammates.find { it.name == name }?.clazz ?: continue
                addTeammate(name, previousClass.name, teammates, networkPlayerInfo) // will add the player with the previous class
            }
            teammates.find { it.name == name }?.isDead = clazz == "DEAD" // set the player as dead if they are
        }
        return teammates
    }

    var dungeonTeammates: List<DungeonPlayer> = emptyList()
    var dungeonTeammatesNoSelf: List<DungeonPlayer> = emptyList()
    var leapTeammates = mutableListOf<DungeonPlayer>()

    private var tickCount = 0
    @SubscribeEvent
    fun UpdateValues(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || mc.theWorld == null || !inDungeons) return
        tickCount++
        if (tickCount % 20 != 0) return

        dungeonTeammates = getDungeonTeammates(dungeonTeammates)
        dungeonTeammatesNoSelf = dungeonTeammates.filter { it.entity != mc.thePlayer }
        leapTeammates = dungeonTeammatesNoSelf.sortedBy { it.clazz }.toMutableList()

        tickCount = 0
    }
}