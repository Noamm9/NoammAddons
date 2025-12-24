package noammaddons.features.impl.dungeons

import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.personalBests
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ActionUtils
import noammaddons.utils.BlockUtils.toPos
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.RenderUtils.drawBox
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawTracer
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color
import kotlin.math.pow
import kotlin.math.sqrt


object M7Relics: Feature("M7 Relics", "A bunch of M7 Relics features") {
    private val relicBox = ToggleSetting("Box Relics")
    private val relicSpawnTimer = ToggleSetting("Spawn Timer")
    private val relicTimer = ToggleSetting("Place Timer")
    private val relicLook = ToggleSetting("Relic Look")
    private val relicLookTime = SliderSetting("Relic Look Time", 0, 250, 10, 150.0).addDependency(relicLook)
    private val blockWrongRelic = ToggleSetting("Block Wrong Relic")

    override fun init() = addSettings(relicBox, relicSpawnTimer, relicTimer, relicLook, relicLookTime)

    private val relicPickUpRegex = Regex("^(\\w{3,16}) picked the Corrupted (.+) Relic!$")
    private val relicTimes = mutableListOf<RelicEntry>()

    private var isP5Active = false
    private var shouldDrawSpawnOutlines = true
    private var spawnTimerTicks = 0
    private var p5StartTime = 0L

    enum class WitherRelic(
        val formalName: String,
        val colorCode: String,
        val cauldronPos: Vec3,
        val visualColor: Color,
        val spawnPos: BlockPos,
        val cauldronCoords: Pair<Int, Int>,
    ) {
        RED("Corrupted Red Relic", "&c", Vec3(51.0, 7.0, 42.0), Color(255, 0, 0, 40), BlockPos(20, 7, 59), 52 to 43),
        ORANGE("Corrupted Orange Relic", "&6", Vec3(57.0, 7.0, 42.0), Color(255, 114, 0, 40), BlockPos(92, 7, 56), 58 to 43),
        GREEN("Corrupted Green Relic", "&a", Vec3(49.0, 7.0, 44.0), Color(0, 255, 0, 40), BlockPos(20, 7, 94), 50 to 45),
        BLUE("Corrupted Blue Relic", "&b", Vec3(59.0, 7.0, 44.0), Color(0, 138, 255, 40), BlockPos(91, 7, 94), 60 to 45),
        PURPLE("Corrupted Purple Relic", "&5", Vec3(54.0, 7.0, 41.0), Color(129, 0, 111, 40), BlockPos(56, 9, 132), 55 to 42);

        val coloredName: String get() = "$colorCode${name.lowercase().replaceFirstChar { it.uppercase() }}"

        companion object {
            fun fromName(name: String): WitherRelic? = entries.find { name.contains(it.formalName) }
        }
    }

    data class RelicEntry(
        val relic: WitherRelic,
        val player: String,
        val pickupTimeMs: Long,
        var placeTimeSeconds: Double = 0.0,
        var isPlaced: Boolean = false,
        var isPB: Boolean = false
    )

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        isP5Active = false
        spawnTimerTicks = 0
        shouldDrawSpawnOutlines = true
        relicTimes.clear()
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        val msg = event.component.noFormatText

        when {
            msg == "[BOSS] Wither King: You... again?" -> {
                if (relicBox.value) shouldDrawSpawnOutlines = false
            }

            msg == "[BOSS] Necron: All this, for nothing..." -> {
                p5StartTime = System.currentTimeMillis()
                isP5Active = true
                if (relicSpawnTimer.value) {
                    spawnTimerTicks = 50
                }
            }

            relicTimer.value -> {
                val match = relicPickUpRegex.find(msg) ?: return
                val (player, relicType) = match.destructured
                val relic = WitherRelic.fromName(relicType) ?: return
                relicTimes.add(RelicEntry(relic, player, System.currentTimeMillis()))
            }
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (spawnTimerTicks > 0) spawnTimerTicks --
    }

    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent) {
        if (! blockWrongRelic.value || F7Phase != 5) return
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return
        val relic = WitherRelic.fromName(mc.thePlayer?.inventory?.getStackInSlot(8)?.displayName?.removeFormatting() ?: return) ?: return

        if (relic.cauldronPos.xCoord.toInt() != event.pos.x || relic.cauldronPos.zCoord.toInt() != event.pos.z) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onPacket(event: MainThreadPacketRecivedEvent.Pre) {
        if (! relicLook.value || F7Phase != 5) return
        val packet = event.packet as? S2FPacketSetSlot ?: return

        val itemStack = packet.func_149174_e() ?: return
        val relic = WitherRelic.fromName(itemStack.displayName.removeFormatting()) ?: return

        // Only rotate for Red/Orange
        if (relic.spawnPos.equalsOneOf(BlockPos(92, 7, 56), BlockPos(20, 7, 59))) {
            ActionUtils.rotateSmoothlyTo(relic.cauldronPos.addVector(0.5, 0.5, 0.5), relicLookTime.value.toLong())
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (! relicSpawnTimer.value || spawnTimerTicks <= 0) return

        val displayTime = (spawnTimerTicks / 20.0).toFixed(2)
        drawCenteredText(
            displayTime,
            mc.getWidth() / 2f,
            mc.getHeight() * 0.4f,
            3f, thePlayer?.clazz?.color ?: Color.WHITE
        )
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (F7Phase != 5) return

        if (relicBox.value) {
            val heldItemName = mc.thePlayer?.inventory?.getStackInSlot(8)?.displayName?.removeFormatting() ?: ""
            WitherRelic.fromName(heldItemName)?.let { heldRelic ->
                drawBlockBox(heldRelic.cauldronPos.toPos(), heldRelic.visualColor, outline = true, fill = true, phase = true)
                drawTracer(heldRelic.cauldronPos.addVector(0.5, 0.5, 0.5), heldRelic.visualColor)
            }
        }

        if (relicBox.value && shouldDrawSpawnOutlines) {
            WitherRelic.entries.forEach { r ->
                drawBox(
                    r.spawnPos.x + 0.25, r.spawnPos.y + 0.3, r.spawnPos.z + 0.25,
                    r.visualColor, outline = true, fill = true, width = 0.5, height = 0.5
                )
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: Tick) {
        if (! relicTimer.value || F7Phase != 5 || relicTimes.isEmpty()) return

        val activeRelics = relicTimes.filter { ! it.isPlaced }
        if (activeRelics.isEmpty()) return

        val armorStands = mc.theWorld.loadedEntityList.filter {
            it is EntityArmorStand && it.getCurrentArmor(3)?.tagCompound?.toString()?.contains("Relic") == true
        }

        for (entity in armorStands) {
            for (entry in activeRelics) {
                if (isEntityAtCauldron(entity.positionVector, entry.relic)) {
                    val currentTime = (System.currentTimeMillis() - p5StartTime) / 1000.0
                    entry.placeTimeSeconds = currentTime.toFixed(2).toDouble()
                    entry.isPlaced = true

                    if (entry.player == mc.session.username) {
                        val data = personalBests.getData()
                        val relicName = entry.relic.formalName
                        val currentPB = data.relics[relicName]

                        if (currentPB == null || entry.placeTimeSeconds < currentPB) {
                            data.relics[relicName] = entry.placeTimeSeconds
                            entry.isPB = true
                            personalBests.save()
                        }
                    }
                }
            }
        }

        if (relicTimes.size == 5 && relicTimes.all { it.isPlaced }) {
            relicTimes.sortedBy { it.placeTimeSeconds }.forEach { entry ->
                val pbSuffix = if (entry.isPB) " &d&l(PB)" else ""
                modMessage("${entry.relic.coloredName} &aRelic placed in &e${entry.placeTimeSeconds}s&a.$pbSuffix")
            }
            relicTimes.clear()
        }
    }

    private fun isEntityAtCauldron(pos: Vec3, relic: WitherRelic): Boolean {
        return sqrt((pos.xCoord - relic.cauldronCoords.first).pow(2.0) + (pos.zCoord - relic.cauldronCoords.second).pow(2.0)) < 1.0
    }
}