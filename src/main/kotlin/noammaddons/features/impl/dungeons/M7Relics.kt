package noammaddons.features.impl.dungeons

import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.personalBests
import noammaddons.ui.config.core.impl.SliderSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ActionUtils
import noammaddons.utils.BlockUtils.toPos
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.MathUtils.add
import noammaddons.utils.MathUtils.distance2D
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawBlockBox
import noammaddons.utils.RenderUtils.drawBox
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawTracer
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color


object M7Relics: Feature(_name = "M7 Relics", desc = "A bunch of M7 Relics features") {
    private data class RelicCauldron(val cauldronPos: Vec3, val color: Color, val relicPos: BlockPos)
    private data class RelicTime(val player: String, val type: String, val pickuptime: Long, var placeTime: String = "", var isPB: Boolean = false)

    private val relicBox = ToggleSetting("Box Relics")
    private val relicSpawnTimer = ToggleSetting("Spawn Timer")
    private val relicTimer = ToggleSetting("Place Timer")
    private val relicLook = ToggleSetting("Relic Look")
    private val relicLookTime = SliderSetting("Relic Look Time", 0, 250, 150.0).addDependency(relicLook)
    override fun init() = addSettings(relicBox, relicSpawnTimer, relicTimer, relicLook, relicLookTime)


    private val relicPickUpRegex = Regex("^(\\w{3,16}) picked the Corrupted (.+) Relic!$")
    private val relicTimes = mutableListOf<RelicTime>()
    private var startTickTimer = false
    private var drawOutline = true
    private var spawnTimerTicks = 0
    private var p5StartTime = 0L

    private val RelicCauldrons = mapOf(
        "Corrupted Blue Relic" to RelicCauldron(Vec3(59.0, 7.0, 44.0), Color(0, 138, 255, 40), BlockPos(91, 7, 94)),
        "Corrupted Orange Relic" to RelicCauldron(Vec3(57.0, 7.0, 42.0), Color(255, 114, 0, 40), BlockPos(92, 7, 56)),
        "Corrupted Purple Relic" to RelicCauldron(Vec3(54.0, 7.0, 41.0), Color(129, 0, 111, 40), BlockPos(56, 9, 132)),
        "Corrupted Red Relic" to RelicCauldron(Vec3(51.0, 7.0, 42.0), Color(255, 0, 0, 40), BlockPos(20, 7, 59)),
        "Corrupted Green Relic" to RelicCauldron(Vec3(49.0, 7.0, 44.0), Color(0, 255, 0, 40), BlockPos(20, 7, 94))
    )

    val relicColors = mapOf(
        "Red" to "&c",
        "Orange" to "&6",
        "Green" to "&a",
        "Blue" to "&b",
        "Purple" to "&5"
    )

    val relicPositions = listOf(
        "&cRed" to (52 to 43),
        "&aGreen" to (50 to 45),
        "&5Purple" to (55 to 42),
        "&6Orange" to (58 to 43),
        "&bBlue" to (60 to 45)
    )

    private fun checkDistance(pos: Vec3, x: Int, z: Int) = distance2D(pos, Vec3(x.toDouble(), .0, z.toDouble())) < 1
    private fun getRelics() = mc.theWorld.loadedEntityList.filterIsInstance<EntityArmorStand>().filter {
        it.getCurrentArmor(3)?.tagCompound.toString().contains("Relic")
    }

    init {
        onWorldLoad {
            startTickTimer = false
            spawnTimerTicks = 0
            drawOutline = true
            relicTimes.clear()
        }

        onChat {
            val msg = it.value

            when (msg) {
                "[BOSS] Wither King: You... again?" -> if (relicBox.value) drawOutline = false
                "[BOSS] Necron: All this, for nothing..." -> {
                    p5StartTime = System.currentTimeMillis()
                    if (relicSpawnTimer.value) {
                        startTickTimer = true
                        spawnTimerTicks = 50
                    }
                }
            }

            if (! relicTimer.value) return@onChat
            val (player, relicType) = relicPickUpRegex.find(msg)?.destructured ?: return@onChat
            relicTimes.add(RelicTime(player, relicColors[relicType] + relicType, System.currentTimeMillis()))
        }

        onServerTick {
            if (! relicSpawnTimer.value) return@onServerTick
            if (! startTickTimer) return@onServerTick
            if (spawnTimerTicks <= 0) return@onServerTick
            spawnTimerTicks --
        }

        onPacket<S2FPacketSetSlot> {
            if (! relicLook.value) return@onPacket
            if (F7Phase != 5) return@onPacket

            RelicCauldrons[it.func_149174_e()?.displayName?.removeFormatting()]?.let { c ->
                if (! c.relicPos.equalsOneOf(BlockPos(92, 7, 56), BlockPos(20, 7, 59))) return@let
                ActionUtils.rotateSmoothlyTo(c.cauldronPos.add(0.5, 0.5, 0.5), relicLookTime.value.toLong())
            }
        }
    }

    @SubscribeEvent
    fun drawTimer(event: RenderOverlay) {
        if (! relicSpawnTimer.value) return
        if (spawnTimerTicks <= 0) return

        drawCenteredText(
            (spawnTimerTicks / 20.0).toFixed(2),
            mc.getWidth() / 2f,
            mc.getHeight() * 0.4f,
            3f, thePlayer?.clazz?.color ?: Color.WHITE
        )
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorld) {
        if (! relicBox.value) return
        if (F7Phase != 5) return

        RelicCauldrons[mc.thePlayer?.inventory?.getStackInSlot(8)?.displayName?.removeFormatting()]?.let { c ->
            drawBlockBox(c.cauldronPos.toPos(), c.color, outline = true, fill = true, phase = true)
            drawTracer(c.cauldronPos.add(Vec3(0.5, 0.5, 0.5)), c.color)
        }

        if (! drawOutline) return
        RelicCauldrons.values.forEach { v ->
            drawBox(
                v.relicPos.x + 0.25,
                v.relicPos.y + 0.3,
                v.relicPos.z + 0.25,
                v.color,
                outline = true, fill = true,
                width = 0.5, height = 0.5
            )
        }
    }

    @SubscribeEvent
    fun onTick(event: Tick) {
        if (! relicTimer.value) return
        if (F7Phase != 5) return
        if (relicTimes.isEmpty()) return

        for (entity in getRelics()) {
            relicPositions.forEach { (type, coords) ->
                val relic = relicTimes.find { it.type == type } ?: return@forEach
                if (relic.placeTime != "") return@forEach
                if (! checkDistance(entity.positionVector, coords.first, coords.second)) return@forEach
                val placeTime = (System.currentTimeMillis() - p5StartTime) / 1000.0
                relic.placeTime = "$placeTime".take(5)

                if (relic.player != mc.session.username) return@forEach
                val data = personalBests.getData()
                val name = relic.type.removeFormatting()
                relic.isPB = relic.let {
                    val me = it.player == mc.session.username
                    val has = data.relics[name] != null
                    val better = if (has) placeTime < data.relics[name] !! else false
                    me && (! has || better)
                }

                if (relic.isPB) {
                    data.relics[name] = placeTime
                    personalBests.save()
                }
            }
        }

        if (relicTimes.any { it.placeTime == "" }) return

        relicTimes.toList().sortedBy { it.placeTime }.forEach { relic ->
            val pbStr = if (relic.isPB) " &d&l(PB)" else ""
            modMessage("${relic.type} &aRelic placed in &e${relic.placeTime}s&a.$pbStr")
        }
        relicTimes.clear()
    }
}