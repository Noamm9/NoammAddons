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
        // === SYSTEM & SERVER MESSAGES ===
        Regex("^Warping you to your SkyBlock island\\.\\.\\.$"),
        Regex("^Warping...$"),
        Regex("Sending to server .+"),
        Regex("Queuing... .+"),
        Regex("Welcome to Hypixel SkyBlock!"),
        Regex("Latest update: SkyBlock .+"),
        Regex("^You are playing on profile: .+$"),
        Regex("^Profile ID: .+$"),
        Regex("OpenGL Error: 1281 \\(Invalid value\\)"),
        Regex("Error initializing players: undefined Hidden"),
        
        // === SECURITY & WARNINGS ===
        Regex("^ {2}Clicking sketchy links can result in your account$"),
        Regex("^ {2}being stolen!$"),
        Regex("^ {2}Link looks suspicious\\? - Don't click it!$"),
        Regex("Blacklisted modifications are a bannable offense!"),
        Regex("\\[WATCHDOG ANNOUNCEMENT]"),
        Regex("^Watchdog has banned .+ players in the last 7 days.$"),
        Regex("Staff have banned an additional .+"),
        
        // === DUNGEON KEYS & DOORS ===
        Regex("A .+ Key was picked up!?"),
        Regex("RIGHT CLICK on .+ to open it\\. This key can only be used to open 1 door!"),
        Regex(".+ opened a .+ door!"),
        Regex("You hear the sound of something opening..."),
        Regex("You do not have the key for this door!"),
        
        // === DUNGEON RESTRICTIONS & RULES ===
        Regex("^A mystical force .+"),
        Regex("You cannot use abilities in this room!"),
        Regex("You cannot do that in this room!"),
        Regex("^You don't have enough charges to break this block right now!$"),
        Regex("There are blocks in the way!"),
        Regex("This creature is immune to this kind of magic!"),
        
        // === CHESTS & LEVERS ===
        Regex("This lever has already been used."),
        Regex("This chest has already been searched!"),
        Regex("You have already opened this dungeon chest!"),
        Regex("Someone has already activated this lever!"),
        Regex("That chest is locked!"),
        
        // === ABILITIES & COOLDOWNS ===
        Regex("This item's ability is temporarily disabled!"),
        Regex("This item is on cooldown.+"),
        Regex("This ability is on cooldown.+"),
        Regex("Your Ultimate is currently on cooldown for .+ more seconds."),
        Regex("You do not have enough mana to do this!"),
        Regex("You need at least .+ mana to activate this!"),
        
        // === ABILITY READY & ACTIVATION ===
        Regex(".+ is ready to use! Press DROP to activate it!"),
        Regex("Throwing Axe is now available!"),
        Regex("Guided Sheep is now available!"),
        Regex("Your Berserk ULTIMATE Ragnarok is now available!"),
        Regex("Used Ragnarok!"),
        Regex("Used Throwing Axe!"),
        
        // === PLAYER READY STATUS ===
        Regex(".+ is now ready!"),
        
        // === CLASS & STATS ===
        Regex("Your .+ stats are doubled because you are the only player using this class!"),
        
        // === MILESTONES & PROGRESSION ===
        Regex(".+ Milestone.+"),
        Regex("^You earned .+ Event EXP from playing SkyBlock!$"),
        Regex("You earned .+ GEXP from playing .+!"),
        Regex("BONUS! Temporarily earn [0-9]+% more skill experience!"),
        
        // === ESSENCE ===
        Regex("ESSENCE! .+ found .+ Essence!"),
        Regex(".+ unlocked .+ Essence.+"),
        Regex(" {4}.+ Essence x.+"),
        Regex(".+ found a Wither Essence! Everyone gains an extra essence!"),
        Regex("You found a Wither Essence! Everyone gains an extra essence!"),
        
        // === DAMAGE TAKEN ===
        Regex("Goldor's TNT Trap hit you for [\\d,.]+ true damage."),
        Regex("Necron's Nuclear Frenzy hit you for .+ damage."),
        Regex("Goldor's Greatsword hit you for .+ damage."),
        Regex("The Frozen Adventurer used Ice Spray on you!"),
        Regex("The Lost Adventurer used Dragon's Breath on you!"),
        Regex("A Crypt Wither Skull exploded, hitting you for .+ damage."),
        Regex("The .+ Trap hit you for .+ damage!"),
        Regex("The Stormy .+ struck you for .+ damage!"),
        Regex("The Flamethrower hit you for .+ damage!"),
        Regex("The Mage's Magma burnt you for .+ true damage."),
        Regex(".+(?:struck|hit|exploded).+ (?:for |you for ).+"),
        
        // === DAMAGE DEALT ===
        Regex("Your .+ hit .+ for [\\d,.]+ damage\\."),
        Regex("Your Guided Sheep hit .+ enemy for .+ damage."),
        Regex("Your Implosion hit .+ enemy for .+ damage."),
        Regex("Your .+ hit .+ (?:enemy|enemies) for .+ damage."),
        Regex("Your Spirit Pet hit .+ enemy for .+ damage."),
        
        // === HEALING ===
        Regex(".+ healed you for .+ health!"),
        Regex("You were healed for .+ health by .+!"),
        Regex("You were healed for .+ health by .+'s Healing Bow and gained \\+.+ Strength for 10 seconds."),
        Regex("Your fairy healed yourself for .+ health!"),
        Regex("Your fairy healed .+ for .+ health!"),
        Regex(".+ fairy healed you for .+ health!"),
        Regex("Your Spirit Pet healed .+ for .+ health!"),
        Regex("Your tether with .+ healed you for .+ health."),
        
        // === BUFFS & EFFECTS ===
        Regex("BUFF! You were splashed by .+ with Healing VIII!"),
        Regex("BUFF! You have gained Healing V!"),
        Regex("You gained .+ HP worth of absorption for 3s from .+!"),
        Regex(".+ granted you .+ strength for 20 seconds!"),
        Regex("Your bone plating reduced the damage you took by .+!"),
        Regex(".+ formed a tether with you!"),
        Regex(".+ used .+ on you!"),
        Regex("Mute silenced you!"),
        
        // === ORBS ===
        Regex(".+ picked up your .+ Orb!"),
        Regex(".+ You picked up a .+ Orb from .+ healing you for .+ and granting you \\+.+% .+ for 10 seconds."),
        
        // === CREEPER VEIL ===
        Regex("^Creeper Veil Activated!$"),
        Regex("^Creeper Veil De-activated!$"),
        Regex("Creeper Veil Activated!"),
        Regex("Creeper Veil De-activated!"),
        
        // === DUNGEON BUFFS & BLESSINGS ===
        Regex("DUNGEON BUFF! .+"),
        Regex("A Blessing of .+ was picked up!"),
        Regex(".+ has obtained Blessing of .+!"),
        Regex(" {5}(?:Also )?(?:grants|granted) you .+"),
        Regex(".*Granted you.+"),
        
        // === ITEM DROPS & OBTAINS ===
        Regex(".+ has obtained Superboom TNT (x[0-9])?!"),
        Regex(".+ has obtained Revive Stone!"),
        Regex(".+ has obtained Premium Flesh!"),
        Regex(".+ has obtained Beating Heart!"),
        Regex("RARE DROP! Hunk of Blue Ice \\(\\+.+% Magic Find!\\)"),
        Regex("RARE DROP! Beating Heart .+"),
        
        // === PUZZLES ===
        Regex("PUZZLE SOLVED!.+"),
        Regex("PUZZLE SOLVED! .+ wasn't fooled by .+! Good job!"),
        Regex("PUZZLE SOLVED! .+ tied Tic Tac Toe! Good job!"),
        
        // === PUZZLES - QUIZ (Oruo) ===
        Regex("\\[STATUE].+"),

        // === PUZZLES - SILVERFISH ===
        Regex("You cannot move the silverfish in that direction!"),
        Regex("You cannot hit the silverfish while it's moving!"),
        
        // === PUZZLES - TIC TAC TOE ===
        Regex("It isn't your turn!"),
        Regex("Don't move diagonally! Bad!"),
        Regex("Oops! You stepped on the wrong block!"),
        
        // === BOSS MESSAGES ===
        Regex("\\[BOSS] .+"),
        
        // === NPC MESSAGES ===
        Regex("\\[NPC] Hugo"),
        Regex(".+ Mort: .+"),
        Regex("\\[NPC] Mort: Talk to me to change your class and ready up\\."),
        
        // === FAIRY MESSAGES ===
        Regex(".+ the Fairy: .+"),
        
        // === SPECIAL MOBS & EVENTS ===
        Regex("^The Redstone Pigmen are unhappy with you stealing their ores! Look out!$"),
        Regex("A shiver runs down your spine..."),
        Regex("Giga Lightning.+"),
        
        // === MISCELLANEOUS NOTIFICATIONS ===
        Regex("\\[SKULL] .+"),
        Regex("\\[BOMB] Creeper:.+"),
        Regex("\\[Sacks] .+"),
        Regex("You summoned your.+"),
        Regex("Your Auto Recombobulator recombobulated"),
        Regex(" Experience Team Bonus"),
        
        // === INVENTORY MANAGEMENT ===
        Regex("Inventory full\\? Don't forget to check out your Storage inside the SkyBlock Menu!"),
        Regex("You cannot put this item in the Potion Bag!"),
        Regex("You don't have any inventory space!"),
        Regex("You have .+ unclaimed .+"),
        
        // === POTION EFFECTS ===
        Regex("Your active Potion Effects have been paused and stored. They will be restored when you leave Dungeons! You are not allowed to use existing Potion Effects while in Dungeons."),
        Regex("You are not allowed to use Potion Effects while in Dungeon, therefore all active effects have been paused and stored\\. They will be restored when you leave Dungeon!"),
        
        // === RATE LIMITING & SPAM ===
        Regex("Whow! Slow down there!"),
        Regex("Woah slow down, you're doing that too fast!"),
        Regex("Command Failed: This command is on cooldown! Try again in about a second!"),
        Regex("Please wait a few seconds between refreshing!"),
        Regex("Please wait a bit before doing this!"),
        
        // === MENUS & UI ===
        Regex("This menu is disabled here!"),
        Regex("This Terminal doesn't seem to be responsive at the moment."),
        Regex("You have 60 seconds to warp out! CLICK to warp now!"),
        
        // === AUTOPET ===
        Regex("Only up to 2 rules may trigger at once!"),
        Regex("Some of your autopet rules did not trigger."),
        
        // === LOBBY & SOCIAL ===
        Regex(".+ joined the lobby!"),
        
        // === EVENTS ===
        Regex("FISHING FESTIVAL The festival is now underway! Break out your fishing rods and watch out for sharks!"),
        Regex("Hoppity's Hunt has begun! Help Hoppity find his Chocolate Rabbit Eggs across SkyBlock each day during the Spring!"),
        
        // === COMBAT - KILL COMBO ===
        Regex("\\+[0-9]+ Kill Combo .+"),
        Regex("Your Kill Combo has expired! You reached a [0-9]+ Kill Combo!")
    )
}