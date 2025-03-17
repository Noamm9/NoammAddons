package noammaddons.features.dungeons

import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.personalBests
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.toPos
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.LocationUtils.F7Phase
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

object M7Relics: Feature() {
    data class RelicCauldron(val cauldronPos: Vec3, val color: Color, val relicPos: BlockPos)

    private val relicPickUpRegex = Regex("^(\\w{3,16}) picked the Corrupted (.+) Relic!$")
    private var p5Started: Long = 0
    private var pickedRelic: Long = 0
    private var spawned: Long = 0
    private var relic: String? = null
    private var scanning = false
    private var startTickTimer = false
    private var ticks = 0
    private var drawOutline = true
    private val relicTimes = mutableMapOf<String, String>()

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

    @SubscribeEvent
    fun onChat(event: Chat) {
        val msg = event.component.noFormatText

        when (msg) {
            "[BOSS] Wither King: You... again?" -> if (config.M7RelicOutline) drawOutline = false
            "[BOSS] Necron: All this, for nothing..." -> {
                if (config.M7RelicSpawnTimer) ticks = 50
                if (config.M7RelicPickupTimer) {
                    p5Started = System.currentTimeMillis()
                    scanning = true
                    startTickTimer = true
                }
            }
        }

        if (! config.M7RelicPickupTimer) return
        if (! msg.matches(relicPickUpRegex)) return
        val (player, relicType) = relicPickUpRegex.find(msg)?.destructured ?: return
        if (player == mc.session.username) {
            pickedRelic = System.currentTimeMillis()
            relic = relicType
        }

    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        p5Started = 0
        pickedRelic = 0
        spawned = 0
        relic = null
        scanning = false
        startTickTimer = false
        ticks = 0
        drawOutline = true
    }

    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent) {
        if (relic == null) return
        if (! event.action.equalsOneOf(PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, PlayerInteractEvent.Action.LEFT_CLICK_BLOCK)) return
        val blockClicked = getBlockAt(event.pos)
        val heldItem = mc.thePlayer.heldItem?.displayName ?: return
        if (blockClicked in listOf(Blocks.anvil, Blocks.cauldron) || ! heldItem.contains("Relic") && ! heldItem.contains("SkyBlock Menu")) return

        relicMessage()
    }

    @SubscribeEvent
    fun onTick(event: Tick) {
        if (scanning) {
            mc.theWorld.loadedEntityList
                .filterIsInstance<EntityArmorStand>()
                .firstOrNull { it.getCurrentArmor(3)?.tagCompound?.toString()?.contains("Relic") == true }
                ?.let {
                    spawned = System.currentTimeMillis()
                    scanning = false
                }
        }

        if (p5Started == 0L) return

        mc.theWorld.loadedEntityList
            .filterIsInstance<EntityArmorStand>()
            .filter { it.getCurrentArmor(3)?.tagCompound?.toString()?.contains("Relic") == true }
            .forEach { entity ->
                val pos = entity.positionVector

                listOf(
                    "&cRed" to (52 to 43),
                    "&aGreen" to (50 to 45),
                    "&5Purple" to (55 to 42),
                    "&6Orange" to (58 to 43),
                    "&bBlue" to (60 to 45)
                ).forEach { (color, coords) ->
                    if (relicTimes[color] == null && checkDistance(pos, coords.first, coords.second)) {
                        relicTimes[color] = ((System.currentTimeMillis() - p5Started) / 1000.0).toString().take(5)
                    }
                }
            }

        if (relicTimes.size == 5) {
            relicTimes.forEach { (key, value) -> modMessage("${relicColors[key] + key} &aRelic placed in &e${value}s&a.") }
            p5Started = 0
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (! config.M7RelicSpawnTimer) return
        if (! startTickTimer) return
        if (ticks <= 0) return
        ticks --
    }

    @SubscribeEvent
    fun drawTimer(event: RenderOverlay) {
        if (! config.M7RelicSpawnTimer) return
        if (ticks <= 0) return

        drawCenteredText(
            (ticks / 20.0).toFixed(2),
            mc.getWidth() / 2f,
            mc.getHeight() * 0.4f,
            3f, thePlayer?.clazz?.color ?: Color.WHITE
        )
    }

    @SubscribeEvent
    fun RelicOutline(event: RenderWorld) {
        if (! config.M7RelicOutline) return
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

    private fun checkDistance(pos: Vec3, x: Int, z: Int) = distance2D(pos, Vec3(x.toDouble(), .0, z.toDouble())) < 1

    private fun relicMessage() {
        val data = personalBests.getData()

        val placeTime = (System.currentTimeMillis() - pickedRelic) / 1000.0
        val sinceP5 = (System.currentTimeMillis() - p5Started) / 1000.0
        val spawnTime = ((spawned - p5Started) / 1000.0).toString().take(5)
        val pickupTime = ((pickedRelic - spawned) / 1000.0).toString().take(5)
        val relicColor = relicColors[relic] ?: ""

        val isPB = relic?.let {
            val better = placeTime < (data.relics[it] ?: Double.MAX_VALUE)
            val has = data.relics[it] != null
            ! has || better
        } ?: false

        if (isPB) {
            personalBests.setData(data)
            personalBests.save()
        }

        if (config.M7RelicSpawnTimer) modMessage("&aRelic took &e${spawnTime}s &ato spawn.")
        if (config.M7RelicPickupTimer) {
            modMessage("&aRelic took &e${pickupTime}s &ato pick up.")
            modMessage("$relicColor$relic Relic &aplaced in &e${placeTime}s&a.${if (isPB) " &d&l(PB)" else ""}")
            modMessage("&aRelic placed &e${sinceP5}s &ainto P5.")
        }

        relic = null
    }
}
