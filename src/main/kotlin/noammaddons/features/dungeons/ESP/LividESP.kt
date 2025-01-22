package noammaddons.features.dungeons.ESP

import kotlinx.coroutines.*
import net.minecraft.block.BlockStainedGlass
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.EnumDyeColor.*
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.DungeonUtils.dungeonTeammates
import noammaddons.utils.EspUtils.EspMob
import noammaddons.utils.LocationUtils.dungeonFloor
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.NumbersUtils.format
import noammaddons.utils.RenderHelper.applyAlpha
import noammaddons.utils.RenderHelper.getRainbowColor
import noammaddons.utils.RenderHelper.getRenderVec
import noammaddons.utils.RenderUtils.drawEntityBox
import noammaddons.utils.RenderUtils.drawString
import noammaddons.utils.RenderUtils.drawTracer
import noammaddons.utils.SoundUtils
import noammaddons.utils.Utils.equalsOneOf


object LividESP: Feature() {
    private var foundLivid = false
    private var livid: EntityOtherPlayerMP? = null
    private var lividTag: Entity? = null
    private var LividJob: Job? = null
    private var lividStart = false
    private var bossTicks = 390
    private var playOnce = false
    private val lividNames = mapOf(
        '2' to "Frog Livid",
        '5' to "Purple Livid",
        '7' to "Doctor Livid",
        '9' to "Scream Livid",
        'a' to "Smile Livid",
        'c' to "Hockey Livid",
        'd' to "Crossed Livid",
        'e' to "Arcade Livid",
        'f' to "Vendetta Livid"
    )
    private val color get() = getRainbowColor(1f)

    @SubscribeEvent
    fun onEvent(event: Event) {
        if (! config.lividFinder) return
        if (dungeonFloor != 5) return
        if (! inBoss) return
        if (! foundLivid) return
        if (livid == null) return
        if (livid?.isDead.equalsOneOf(null, true)) return
        if (livid?.isPlayerSleeping.equalsOneOf(null, true)) return

        when (event) {
            is PostRenderEntityModelEvent -> {
                if (config.espType == 1) return
                if (event.entity != livid) return

                EspMob(event, color)
            }

            is RenderEntityEvent -> {
                if (! config.hideWrongLivids) return
                if (event.entity == livid) return
                if (dungeonTeammates.map { it.entity?.entityId }.contains(event.entity.entityId)) return
                if (event.entity is EntityArmorStand && ! event.entity.name.contains("Livid")) return

                event.isCanceled = true
            }

            is RenderWorld -> {
                if (config.espType == 1) drawEntityBox(livid !!, color.applyAlpha(77))
                drawTracer(livid !!.getRenderVec().add(Vec3(.0, 0.9, .0)), color)

                drawString(
                    format(livid !!.health),
                    livid !!.getRenderVec().add(Vec3(.0, 3.0, .0)),
                    color, 1.5f
                )
            }
        }
    }

    @SubscribeEvent
    @OptIn(DelicateCoroutinesApi::class)
    fun onTick(event: Tick) {
        if (! config.lividFinder) return
        if (dungeonFloor != 5) return
        if (! inBoss) return
        if (foundLivid) return

        val loadedLivids = mc.theWorld.loadedEntityList.filter {
            it.name.contains("Livid") && it.name.length > 5 && it.name[1] == it.name[5]
        }

        if (loadedLivids.size <= 8) return

        lividTag = loadedLivids[0]
        livid = closestLivid(lividTag !!.name[5])
        if (livid != null) foundLivid = true
        if (LividJob != null && LividJob?.isActive == true) return

        LividJob = scope.launch {
            delay(1500)
            val state = mc.theWorld.getBlockState(BlockPos(5, 109, 42))
            val color = state.getValue(BlockStainedGlass.COLOR)
            livid = closestLivid(
                when (color) {
                    GREEN -> '2'
                    PURPLE -> '5'
                    GRAY -> '7'
                    BLUE -> '9'
                    LIME -> 'a'
                    MAGENTA -> 'd'
                    YELLOW -> 'e'
                    RED -> 'c'
                    WHITE -> 'f'
                    else -> {
                        modMessage("Error encountered during Livid Check with color:" + color.name)
                        return@launch
                    }
                }
            ) ?: return@launch
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Unload) {
        foundLivid = false
        livid = null
        lividStart = false
        bossTicks = 390
        playOnce = false
    }

    @SubscribeEvent
    fun onSvrTick(event: ServerTick) {
        if (! lividStart) return
        bossTicks --

        if (bossTicks == 0) {
            lividStart = false
            bossTicks = 390
            showTitle("Ice Spray Livid!", rainbow = true)
            SoundUtils.Pling.start()
        }
    }

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! config.lividFinder) return
        if (dungeonFloor != 5) return
        if (event.component.noFormatText
            != "[BOSS] Livid: Welcome, you've arrived right on time. I am Livid, the Master of Shadows."
        ) return

        lividStart = true
    }

    private fun closestLivid(chatFormatting: Char): EntityOtherPlayerMP? {
        return mc.theWorld.loadedEntityList.filterIsInstance<EntityOtherPlayerMP>()
            .filter { it.name.equals(lividNames[chatFormatting]) }
            .sortedWith(Comparator.comparingDouble { lividTag !!.getDistanceSqToEntity(it) }).firstOrNull()
    }
}