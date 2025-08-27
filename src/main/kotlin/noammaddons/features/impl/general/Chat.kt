package noammaddons.features.impl.general

import gg.essential.elementa.utils.withAlpha
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.distance3D
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.favoriteColor
import java.util.concurrent.CopyOnWriteArrayList


object Chat: Feature() {
    data class Waypoint(val name: String, val loc: Vec3)

    private val hideUseless = ToggleSetting("Hide Useless Messages", false)
    private val printSbEXP = ToggleSetting("Print SkyBlock XP", false)

    private val chatWaypoints = ToggleSetting("Chat Waypoints", true)
    private val drawName = ToggleSetting("Sender Name", true).addDependency(chatWaypoints)
    private val drawTracer = ToggleSetting("Tracer", true).addDependency(chatWaypoints)
    private val drawBox = ToggleSetting("Box").addDependency(chatWaypoints)
    private val removeTimeout = SliderSetting("Remove Timeout (in seconds)", 10f, 120f, 1, 30.0).addDependency(chatWaypoints)
    private val removeOnReach = ToggleSetting("Remove On Reach", true).addDependency(chatWaypoints)

    private val nameColor = ColorSetting("Sender Name Color", favoriteColor, false).addDependency(chatWaypoints).addDependency(drawName)
    private val tracerColor = ColorSetting("Tracer Color", favoriteColor, false).addDependency(chatWaypoints).addDependency(drawTracer)
    private val boxColor = ColorSetting("Box Color", favoriteColor.withAlpha(0.3f)).addDependency(chatWaypoints).addDependency(drawBox)

    override fun init() {
        addSettings(
            SeperatorSetting("Spam Protection"),
            hideUseless, printSbEXP,
            SeperatorSetting("Waypoint"),
            drawName, drawTracer, drawBox,
            removeTimeout, removeOnReach,
            SeperatorSetting("Colors"),
            nameColor, tracerColor,
            boxColor
        )
    }

    private val waypoints = CopyOnWriteArrayList<Waypoint>()
    private val skyBlockExpRegex = Regex(".*(§b\\+\\d+ SkyBlock XP §.\\([^()]+\\)§b \\(\\d+/\\d+\\)).*")
    private val coordRegex = Regex("^(Co-op|Party)?(?: > )?(?:\\[\\d+] .? ?)?(?:\\[[\\w+]+] )?(\\w{1,16}): x: (.{1,4}), y: (.{1,4}), z: (.{1,4})")
    private var lastMatch: String? = null

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! chatWaypoints.value) return
        val match = coordRegex.find(event.component.noFormatText) ?: return
        val (_, name, x, y, z) = match.destructured
        val waypoint = Waypoint(name, Vec3(x.toDouble(), y.toDouble(), z.toDouble()))
        setTimeout(removeTimeout.value.toLong() * 1000) { waypoints.remove(waypoint) } // lazy ass way ik XD
        waypoints.add(waypoint)
    }

    @SubscribeEvent
    fun renderWaypoints(event: RenderWorld) {
        if (waypoints.isEmpty()) return

        waypoints.forEach { waypoint ->
            val distance = distance3D(mc.thePlayer.renderVec, waypoint.loc).toFloat()
            var scale = (distance * 0.2f).coerceIn(2f, 10f)
            if (distance < 10) scale = 2f

            if (drawBox.value) RenderUtils.drawBlockBox(
                BlockPos(waypoint.loc),
                boxColor.value,
                outline = true,
                fill = true,
                phase = true
            )

            if (drawName.value) RenderUtils.drawString(
                waypoint.name,
                waypoint.loc.add(x = 0.5, y = 1 + distance * 0.01f, z = 0.5),
                nameColor.value,
                scale, phase = true
            )

            if (drawTracer.value) RenderUtils.drawTracer(
                waypoint.loc.add(0.5, 0.5, 0.5),
                tracerColor.value
            )

            if (distance <= 10 && removeOnReach.value) waypoints.remove(waypoint)
        }
    }

    @SubscribeEvent
    fun onNewChatMessage(event: AddMessageToChatEvent) {
        if (! hideUseless.value) return
        if (regex.any { it.matches(event.component.noFormatText) }) return event.setCanceled(true)
        if (formattedRegexs.any { it.matches(event.component.noFormatText) }) return event.setCanceled(true)
    }

    @SubscribeEvent
    fun onActionbar(event: Actionbar) {
        if (! inSkyblock) return
        if (! printSbEXP.value) return
        val match = skyBlockExpRegex.find(event.component.formattedText)?.groupValues?.get(1) ?: return
        if (match != lastMatch) {
            lastMatch = match
            modMessage(match)
        }
    }

    private val regex = listOf(
        Regex("^ {2}Clicking sketchy links can result in your account$"),
        Regex("^ {2}being stolen!$"),
        Regex("^ {2}Link looks suspicious\\? - Don't click it!$"),
        Regex("^Creeper Veil Activated!$"),
        Regex("^The Redstone Pigmen are unhappy with you stealing their ores! Look out!$"),
        Regex("^Creeper Veil De-activated!$"),
        Regex("^A Wither Key was picked up!$"),
        Regex("^Warping you to your SkyBlock island\\.\\.\\.$"),
        Regex("^You earned .+ Event EXP from playing SkyBlock!$"),
        Regex("^Warping...$"),
        Regex("^Watchdog has banned .+ players in the last 7 days.$"),
        Regex("^You are playing on profile: .+$"),
        Regex("^Profile ID: .+$"),
        Regex("Goldor's TNT Trap hit you for [\\d,.]+ true damage."),
        Regex("A Blood Key was picked up"),
        Regex("This Terminal doesn't seem to be responsive at the moment."),
        Regex("Whow! Slow down there!"),
        Regex("Giga Lightning.+"),
        Regex("Necron's Nuclear Frenzy hit you for .+ damage."),
        Regex("Woah slow down, you're doing that too fast!"),
        Regex("Command Failed: This command is on cooldown! Try again in about a second!"),
        Regex("Someone has already activated this lever!"),
        Regex("Goldor's Greatsword hit you for .+ damage."),
        Regex("A mystical force in this room prevents you from using that ability!"),
        Regex("The Frozen Adventurer used Ice Spray on you!"),
        Regex("It isn't your turn!"),
        Regex("That chest is locked!"),
        Regex("Don't move diagonally! Bad!"),
        Regex("Oops! You stepped on the wrong block!"),
        Regex("Used Ragnarok!"),
        Regex("Your Auto Recombobulator recombobulated"),
        Regex("You cannot use abilities in this room!"),
        Regex("A shiver runs down your spine..."),
        Regex(".*Granted you.+"),
        Regex("Blacklisted modifications are a bannable offense!"),
        Regex("\\[WATCHDOG ANNOUNCEMENT]"),
        Regex("Staff have banned an additional .+"),
        Regex("Your Ultimate is currently on cooldown for .+ more seconds."),
        Regex("ESSENCE! .+ found .+ Essence!"),
        Regex("This lever has already been used."),
        Regex("You hear the sound of something opening..."),
        Regex("This chest has already been searched!"),
        Regex("A Blessing of .+ was picked up!"),
        Regex("The Lost Adventurer used Dragon's Breath on you!"),
        Regex("Your Berserk ULTIMATE Ragnarok is now available!"),
        Regex("This item's ability is temporarily disabled!"),
        Regex("Throwing Axe is now available!"),
        Regex("Used Throwing Axe!"),
        Regex("OpenGL Error: 1281 \\(Invalid value\\)"),
        Regex("You have 60 seconds to warp out! CLICK to warp now!"),
        Regex("\\[STATUE].+"),
        Regex("\\[NPC] Hugo"),
        Regex("PUZZLE SOLVED!.+"),
        Regex("DUNGEON BUFF! .+"),
        Regex("A Crypt Wither Skull exploded, hitting you for .+ damage."),
        Regex(".+ opened a WITHER door!"),
        Regex("\\[SKULL] .+"),
        Regex("You summoned your.+"),
        Regex("\\[BOMB] Creeper:.+"),
        Regex("\\[Sacks] .+ item.+"),
        Regex("The .+ Trap hit you for .+ damage!"),
        Regex("Healer Milestone.+"),
        Regex("Archer Milestone.+"),
        Regex("Mage Milestone.+"),
        Regex("Tank Milestone.+"),
        Regex("Berserk Milestone.+"),
        Regex("There are blocks in the way!"),
        Regex("Error initializing players: undefined Hidden"),
        Regex("Your .+ stats are doubled because you are the only player using this class!"),
        Regex("You have .+ unclaimed .+"),
        Regex(".+ joined the lobby! .+"),
        Regex("Welcome to Hypixel SkyBlock!"),
        Regex("Latest update: SkyBlock .+"),
        Regex("BONUS! Temporarily earn 5% more skill experience!"),
        Regex(".+ is now ready!"),
        Regex("Sending to server .+"),
        Regex("Queuing... .+"),
        Regex(".+ Milestone .+: .+"),
        Regex("Your CLASS stats are doubled because you are the only player using this class!"),
        Regex("\\[BOSS] .+"),
        Regex(".+ Mort: .+"),
        Regex("\\[NPC] Mort: Talk to me to change your class and ready up\\."),
        Regex("You do not have enough mana to do this!"),
        Regex("Thunderstorm is ready to use! Press DROP to activate it!"),
        Regex(".+ unlocked .+ Essence!"),
        Regex(".+ unlocked .+ Essence x\\d+!"),
        Regex("This menu is disabled here!"),
        Regex("This item is on cooldown.+"),
        Regex("This ability is on cooldown.+"),
        Regex("The Stormy .+ struck you for .+ damage!"),
        Regex("Please wait a few seconds between refreshing!"),
        Regex("You cannot move the silverfish in that direction!"),
        Regex("You cannot hit the silverfish while it's moving!"),
        Regex("Your active Potion Effects have been paused and stored. They will be restored when you leave Dungeons! You are not allowed to use existing Potion Effects while in Dungeons."),
        Regex("The Flamethrower hit you for .+ damage!"),
        Regex(".+ found a Wither Essence! Everyone gains an extra essence!"),
        Regex("Ragnarok is ready to use! Press DROP to activate it!"),
        Regex("This creature is immune to this kind of magic!"),
        Regex("FISHING FESTIVAL The festival is now underway! Break out your fishing rods and watch out for sharks!"),
        Regex("Only up to 2 rules may trigger at once!"),
        Regex("Some of your autopet rules did not trigger."),
        Regex("Inventory full\\? Don't forget to check out your Storage inside the SkyBlock Menu!"),
        Regex("You cannot put this item in the Potion Bag!"),
        Regex("Please wait a bit before doing this!"),
        Regex("You don't have any inventory space!"),
        Regex("Your .+ hit .+ for [\\d,.]+ damage\\."),
        Regex(".+ healed you for .+ health!"),
        Regex("You earned .+ GEXP from playing .+!"),
    )

    private val formattedRegexs = listOf(
        Regex("(.*) §r§eunlocked §r§d(.*) Essence §r§8x(.*)§r§e!"),
        Regex(" {4}§r§d(.*) Essence §r§8x(.*)"),
        Regex(" Experience §r§b(Team Bonus)"),
        Regex("§7Your Guided Sheep hit §r§c(.*) §r§7enemy for §r§c(.*) §r§7damage."),
        Regex("§a§lBUFF! §fYou were splashed by (.*) §fwith §r§cHealing VIII§r§f!"),
        Regex("§aYou were healed for (.*) health by (.*)§a!"),
        Regex("§aYou gained (.*) HP worth of absorption for 3s from §r(.*)§r§a!"),
        Regex("§c(.*) §r§epicked up your (.*) Orb!"),
        Regex("§cThis ability is on cooldown for (.*)s."),
        Regex("§a§l(.*) healed you for (.*) health!"),
        Regex("§eYour bone plating reduced the damage you took by §r§c(.*)§r§e!"),
        Regex("(.*) §r§eformed a tether with you!"),
        Regex("§eYour tether with (.*) §r§ehealed you for §r§a(.*) §r§ehealth."),
        Regex("§7Your Implosion hit §r§c(.*) §r§7enemy for §r§c(.*) §r§7damage."),
        Regex("§eYour §r§6Spirit Pet §r§ehealed (.*) §r§efor §r§a(.*) §r§ehealth!"),
        Regex("§eYour §r§6Spirit Pet §r§ehit (.*) enemy for §r§c(.*) §r§edamage."),
        Regex("§cYou need at least (.*) mana to activate this!"),
        Regex("§eYou were healed for §r§a(.*)§r§e health by §r(.*)§r§e's §r§9Healing Bow§r§e and gained §r§c\\+(.*) Strength§r§e for 10 seconds."),
        Regex("(.*)§r§a granted you §r§c(.*) §r§astrength for §r§e20 §r§aseconds!"),
        Regex("§eYour fairy healed §r§ayourself §r§efor §r§a(.*) §r§ehealth!"),
        Regex("§eYour fairy healed §r(.*) §r§efor §r§a(.*) §r§ehealth!"),
        Regex("(.*) fairy healed you for §r§a(.*) §r§ehealth!"),
        Regex("§a§r§6Guided Sheep §r§ais now available!"),
        Regex("§dCreeper Veil §r§aActivated!"),
        Regex("§dCreeper Veil §r§cDe-activated!"),
        Regex("§6Rapid Fire§r§a is ready to use! Press §r§6§lDROP§r§a to activate it!"),
        Regex("§6Castle of Stone§r§a is ready to use! Press §r§6§lDROP§r§a to activate it!"),
        Regex("§6Ragnarok§r§a is ready to use! Press §r§6§lDROP§r§a to activate it!"),
        Regex("(.*) §r§aused §r(.*) §r§aon you!"),
        Regex("§cThe (.*)§r§c struck you for (.*) damage!"),
        Regex("§cThe (.*) hit you for (.*) damage!"),
        Regex("§7(.*) struck you for §r§c(.*)§r§7 damage."),
        Regex("(.*) hit you for §r§c(.*)§r§7 damage."),
        Regex("(.*) hit you for §r§c(.*)§r§7 true damage."),
        Regex("§7(.*) exploded, hitting you for §r§c(.*)§r§7 damage."),
        Regex("(.*)§r§c hit you with §r(.*) §r§cfor (.*) damage!"),
        Regex("(.*)§r§a struck you for §r§c(.*)§r§a damage!"),
        Regex("(.*)§r§c struck you for (.*)!"),
        Regex("§7The Mage's Magma burnt you for §r§c(.*)§r§7 true damage."),
        Regex("§7Your (.*) hit §r§c(.*) §r§7(enemy|enemies) for §r§c(.*) §r§7damage."),
        Regex("§cMute silenced you!"),
        Regex("§cYou cannot hit the silverfish while it's moving!"),
        Regex("§cYou cannot move the silverfish in that direction!"),
        Regex("§cThis chest has already been searched!"),
        Regex("§cThis lever has already been used."),
        Regex("§cYou cannot do that in this room!"),
        Regex("§cYou do not have the key for this door!"),
        Regex("§cYou have already opened this dungeon chest!"),
        Regex("§cYou cannot use abilities in this room!"),
        Regex("§cA mystical force in this room prevents you from using that ability!"),
        Regex("§6§lDUNGEON BUFF! (.*) §r§ffound a §r§dBlessing of (.*)§r§f!(.*)"),
        Regex("§6§lDUNGEON BUFF! §r§fYou found a §r§dBlessing of (.*)§r§f!(.*)"),
        Regex("§6§lDUNGEON BUFF! §r§fA §r§dBlessing of (.*)§r§f was found! (.*)"),
        Regex("§eA §r§a§r§dBlessing of (.*)§r§e was picked up!"),
        Regex("(.*) §r§ehas obtained §r§a§r§dBlessing of (.*)§r§e!"),
        Regex(" {5}§r§7Granted you §r§a§r§a(.*)§r§7 & §r§a(.*)x §r§c❁ Strength§r§7."),
        Regex(" {5}§r§7Also granted you §r§a§r§a(.*)§r§7 & §r§a(.*)x §r§9☠ Crit Damage§r§7."),
        Regex(" {5}§r§7(Grants|Granted) you §r§a(.*) Defense §r§7and §r§a+(.*) Damage§r§7."),
        Regex(" {5}§r§7Granted you §r§a§r§a(.*)x HP §r§7and §r§a§r§a(.*)x §r§c❣ Health Regen§r§7."),
        Regex(" {5}§r§7(Grants|Granted) you §r§a(.*) Intelligence §r§7and §r§a+(.*)? Speed§r§7."),
        Regex(" {5}§r§7Granted you §r§a+(.*) HP§r§7, §r§a(.*) Defense§r§7, §r§a(.*) Intelligence§r§7, and §r§a(.*) Strength§r§7."),
        Regex("§a§lBUFF! §fYou have gained §r§cHealing V§r§f!"),
        Regex("§a§lPUZZLE SOLVED! (.*) §r§ewasn't fooled by §r§c(.*)§r§e! §r§4G§r§co§r§6o§r§ed§r§a §r§2j§r§bo§r§3b§r§5!"),
        Regex("§a§lPUZZLE SOLVED! (.*) §r§etied Tic Tac Toe! §r§4G§r§co§r§6o§r§ed§r§a §r§2j§r§bo§r§3b§r§5!"),
        Regex("§4\\[STATUE] Oruo the Omniscient§r§f: §r(.*) §r§fthinks the answer is §r§6 . §r(.*)§r§f! §r§fLock in your party's answer in my Chamber!"),
        Regex("§4\\[STATUE] Oruo the Omniscient§r§f: §r§fThough I sit stationary in this prison that is §r§cThe Catacombs§r§f, my knowledge knows no bounds."),
        Regex("§4\\[STATUE] Oruo the Omniscient§r§f: §r§fProve your knowledge by answering 3 questions and I shall reward you in ways that transcend time!"),
        Regex("§4\\[STATUE] Oruo the Omniscient§r§f: §r§fAnswer incorrectly, and your moment of ineptitude will live on for generations."),
        Regex("§4\\[STATUE] Oruo the Omniscient§r§f: §r§f2 questions left... Then you will have proven your worth to me!"),
        Regex("§4\\[STATUE] Oruo the Omniscient§r§f: §r§fOne more question!"),
        Regex("§4\\[STATUE] Oruo the Omniscient§r§f: §r§fI bestow upon you all the power of a hundred years!"),
        Regex("§4\\[STATUE] Oruo the Omniscient§r§f: §r§fYou've already proven enough to me! No need to press more of my buttons!"),
        Regex("§4\\[STATUE] Oruo the Omniscient§r§f: §r§fI've had enough of you and your party fiddling with my buttons. Scram!"),
        Regex("§4\\[STATUE] Oruo the Omniscient§r§f: §r§fEnough! My buttons are not to be pressed with such lack of grace!"),
        Regex("§5A shiver runs down your spine..."),
        Regex("§e§lRIGHT CLICK §r§7on §r§7a §r§8WITHER §r§7door§r§7 to open it. This key can only be used to open §r§a1§r§7 door!"),
        Regex("§e§lRIGHT CLICK §r§7on §r§7the §r§cBLOOD DOOR§r§7 to open it. This key can only be used to open §r§a1§r§7 door!"),
        Regex("(.*) §r§ehas obtained §r§a§r§9Superboom TNT§r§e!"),
        Regex("(.*) §r§ehas obtained §r§a§r§9Superboom TNT §r§8x2§r§e!"),
        Regex("§6§lRARE DROP! §r§9Hunk of Blue Ice §r§b\\(+(.*)% Magic Find!\\)"),
        Regex("(.*) §r§ehas obtained §r§a§r§6Revive Stone§r§e!"),
        Regex("(.*) §r§ffound a §r§dWither Essence§r§f! Everyone gains an extra essence!"),
        Regex("§d(.*) the Fairy§r§f: You killed me! Take this §r§6Revive Stone §r§fso that my death is not in vain!"),
        Regex("§d(.*) the Fairy§r§f: You killed me! I'll revive you so that my death is not in vain!"),
        Regex("§d(.*) the Fairy§r§f: You killed me! I'll revive your friend §r(.*) §r§fso that my death is not in vain!"),
        Regex("§d(.*) the Fairy§r§f: Have a great life!"),
        Regex("§c(.*) §r§eYou picked up a (.*) Orb from (.*) §r§ehealing you for §r§c(.*) §r§eand granting you +(.*)% §r§e(.*) for §r§b10 §r§eseconds."),
        Regex("(.*) §r§ehas obtained §r§a§r§9Premium Flesh§r§e!"),
        Regex("§6§lRARE DROP! §r§9Beating Heart §r§b(.*)"),
        Regex("(.*) §r§ehas obtained §r§a§r§9Beating Heart§r§e!"),
        Regex("§fYou found a §r§dWither Essence§r§f! Everyone gains an extra essence!")
    )
}