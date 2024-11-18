package noammaddons.utils

import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Tick
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.TablistUtils.getTabList
import java.awt.Color


// Big Thanks to Odin for Having their code available.
// https://github.com/Noamm9/OdinClient/blob/main/odinmain/src/main/kotlin/me/odinmain/utils/skyblock/dungeon/DungeonUtils.kt
// @Modified
object DungeonUtils {
    /**
     * Enumeration representing player classes in a dungeon setting.
     *
     * Each class is associated with a specific code and color used for formatting in the game. The classes include
     * Archer, Mage, Berserk, Healer, and Tank.
     *
     * @property color The color associated with the class.
     * */
    enum class Classes(val color: Color) {
        Archer(Color(255, 0, 0)),
        Berserk(Color(255, 106, 0)),
        Healer(Color(255, 0, 255)),
        Mage(Color(0, 255, 255)),
        Tank(Color(0, 255, 0)),
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


    private fun getDungeonTabList(): List<Pair<NetworkPlayerInfo, String>>? {
        val tabEntries = getTabList
        if (tabEntries.size < 18 || ! tabEntries[0].second.contains("§r§b§lParty §r§f(")) return null
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
        if (config.DevMode) return listOf(
            DungeonPlayer("Deitee__", Classes.Mage, entity = Player),
            DungeonPlayer("Noamm9", Classes.Archer),
            DungeonPlayer("hellop2", Classes.Healer),
            DungeonPlayer("Ori", Classes.Tank),
        )

        val teammates = mutableListOf<DungeonPlayer>()
        val tabList = getDungeonTabList() ?: return emptyList()

        for ((networkPlayerInfo, line) in tabList) {

            val (_, _, name, clazz, _) = tablistRegex.find(line.removeFormatting())?.groupValues ?: continue

            addTeammate(name, clazz, teammates, networkPlayerInfo)
            if (clazz == "DEAD" || clazz == "EMPTY") {
                val previousClass = previousTeammates.find { it.name == name }?.clazz ?: continue
                addTeammate(name, previousClass.name, teammates, networkPlayerInfo)
            }
            teammates.find { it.name == name }?.isDead = clazz == "DEAD"
        }
        return teammates
    }

    var dungeonTeammates: List<DungeonPlayer> = emptyList()
    var dungeonTeammatesNoSelf: List<DungeonPlayer> = emptyList()
    var leapTeammates = mutableListOf<DungeonPlayer>()
    var thePlayer: DungeonPlayer? = null

    private var tickCount = 0

    @SubscribeEvent
    fun updateValues(event: Tick) {
        if (mc.theWorld == null || ! inDungeons) return
        tickCount ++
        if (tickCount % 20 != 0) return

        dungeonTeammates = getDungeonTeammates(dungeonTeammates)
        dungeonTeammatesNoSelf = dungeonTeammates.filter { it.entity != Player }
        thePlayer = dungeonTeammates.find { it.entity == Player }
        leapTeammates = dungeonTeammatesNoSelf.sortedBy { it.clazz }.toMutableList()

        tickCount = 0
    }

    @SubscribeEvent
    fun reset(event: WorldEvent.Unload) {
        dungeonTeammates = emptyList()
        dungeonTeammatesNoSelf = emptyList()
        leapTeammates = mutableListOf()
    }
}