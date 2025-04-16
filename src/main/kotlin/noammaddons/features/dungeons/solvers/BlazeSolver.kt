package noammaddons.features.dungeons.solvers

import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityBlaze
import net.minecraft.init.Blocks
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.noammaddons.Companion.personalBests
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.ChatUtils.clickableChat
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.EspUtils.espMob
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.RenderUtils.draw3DLine
import noammaddons.utils.ScanUtils.getRoomCenterAt
import noammaddons.utils.Utils.formatPbPuzzleMessage
import noammaddons.utils.Utils.remove


object BlazeSolver: Feature() {
    private val BlazeHpRegex = Regex("""^\[Lv15] Blaze [\d,]+/([\d,]+)❤$""")
    private var inBlaze = false
    private val blazes = mutableListOf<Entity>()
    private var lastBlazeCount = 10
    private val hpMap = mutableMapOf<Entity, Int>()
    private var reversed = false
    private var trueTimeStarted: Long? = null
    private var timeStarted: Long? = null


    @SubscribeEvent
    fun onEnter(event: DungeonEvent.RoomEvent.onEnter) {
        if (! config.BlazeSolver) return
        if (! event.room.name.contains("Blaze")) return

        inBlaze = true
        val (X, Z) = getRoomCenterAt(mc.thePlayer.position)
        reversed = getBlockAt(X + 1, 118, Z) != Blocks.cobblestone
        trueTimeStarted = System.currentTimeMillis()
        lastBlazeCount = 10
    }

    @SubscribeEvent
    fun solveBlaze(event: Tick) {
        if (! inBlaze) return

        blazes.clear()
        hpMap.clear()

        mc.theWorld.loadedEntityList.filterIsInstance<EntityArmorStand>().forEach { entity ->
            val match = BlazeHpRegex.find(entity.displayName.noFormatText) ?: return@forEach
            val health = match.groupValues[1].remove(",").toIntOrNull() ?: return@forEach
            val possibleEntity = entity.entityWorld.getEntitiesInAABBexcluding(
                entity, entity.entityBoundingBox.offset(0.0, - 1.0, 0.0)
            ) { it is EntityBlaze }.firstOrNull() ?: return@forEach

            if (blazes.contains(possibleEntity) || hpMap.keys.contains(possibleEntity)) return@forEach
            hpMap[possibleEntity] = health
            blazes.add(possibleEntity)
        }

        blazes.sortWith(Comparator.comparingInt { hpMap[it] !! })
        if (blazes.isNotEmpty() && reversed) blazes.reverse()

        if (blazes.size == 10 && trueTimeStarted == null) trueTimeStarted = System.currentTimeMillis()
        if (blazes.size == 9 && timeStarted == null) timeStarted = System.currentTimeMillis()


        if (blazes.isEmpty() && lastBlazeCount == 1) {
            val personalBestsData = personalBests.getData().pazzles
            val previousBest = personalBestsData["Blaze"]
            val completionTime = (System.currentTimeMillis() - timeStarted !!).toDouble()
            val totalTime = (System.currentTimeMillis() - trueTimeStarted !!).toDouble()

            val message = formatPbPuzzleMessage("Blaze", completionTime, previousBest)

            sendPartyMessage(message)

            clickableChat(
                msg = message,
                cmd = "/na copy ${message.removeFormatting()}",
                hover = "Total Time: &b${(totalTime / 1000.0).toFixed(2)}s",
                prefix = false
            )

            lastBlazeCount = 0
        }

        lastBlazeCount = blazes.size
    }

    @SubscribeEvent
    fun onExit(event: DungeonEvent.RoomEvent.onExit) {
        if (! config.BlazeSolver) return
        if (! inBlaze) return

        inBlaze = false
        blazes.clear()
        hpMap.clear()
        trueTimeStarted = null
        timeStarted = null
    }

    @SubscribeEvent
    fun renderBlazeSolve(e: RenderWorld) {
        if (! config.BlazeSolver) return
        if (! inDungeon) return
        if (inBoss) return
        if (blazes.isEmpty()) return

        blazes.withIndex().forEach { (i, entity) ->
            espMob(entity, getBlazeColor(i))

            if (i in 1 .. 2) {
                val b1 = blazes[i - 1].positionVector.add(Vec3(.0, blazes[i - 1].height / 2.0, .0))
                val b2 = blazes[i].positionVector.add(Vec3(.0, blazes[i].height / 2.0, .0))
                draw3DLine(b1, b2, config.BlazeSolverLineColor, 3f)
            }
        }
    }

    fun getBlazeColor(index: Int) = when (index) {
        0 -> config.BlazeSolverFirstBlazeColor
        1 -> config.BlazeSolverSecondBlazeColor
        else -> config.BlazeSolverThirdBlazeColor
    }
}
