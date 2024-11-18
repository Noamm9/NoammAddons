package noammaddons.features.dungeons.ESP

import net.minecraft.block.BlockStainedGlass
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.item.EnumDyeColor
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderEntityModelEvent
import noammaddons.events.RenderWorld
import noammaddons.events.Tick
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.DungeonUtils.dungeonTeammates
import noammaddons.utils.EspUtils.EspMob
import noammaddons.utils.LocationUtils.dungeonFloor
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.RenderHelper.applyAlpha
import noammaddons.utils.RenderHelper.getRainbowColor
import noammaddons.utils.RenderHelper.getRenderVec
import noammaddons.utils.RenderUtils.drawEntityBox
import noammaddons.utils.RenderUtils.drawTracer


object LividESP: Feature() {
    private var foundLivid = false
    private var livid: Entity? = null
    private var lividTag: Entity? = null
    private var thread: Thread? = null
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

    @SubscribeEvent
    fun onRenderEntity(event: RenderEntityModelEvent) {
        if (! config.lividFinder) return
        if (dungeonFloor != 5) return
        if (! foundLivid) return
        if (config.espType == 1) return
        if (event.entity != livid) return

        EspMob(event, getRainbowColor(1f))
    }

    @SubscribeEvent
    fun onRenderEntity(event: RenderLivingEvent.Pre<*>) {
        if (! config.lividFinder) return
        if (dungeonFloor != 5) return
        if (! foundLivid) return
        if (config.espType == 1) return
        if (event.entity == livid) return
        if (dungeonTeammates.map { it.entity?.entityId }.contains(event.entity.entityId)) return

        event.isCanceled = config.hideWrongLivids
    }


    @SubscribeEvent
    fun onRender(event: RenderWorld) {
        if (! config.lividFinder) return
        if (dungeonFloor != 5) return
        if (! foundLivid) return
        if (livid == null) return
        if (livid?.isDead == true) return
        if (config.espType == 1) drawEntityBox(livid !!, getRainbowColor(1f).applyAlpha(77))

        drawTracer(livid !!.getRenderVec().add(Vec3(.0, 0.9, .0)), getRainbowColor(1f))
    }


    @SubscribeEvent
    fun onTick(event: Tick) {
        if (! config.lividFinder || dungeonFloor != 5) return
        if (foundLivid || ! inBoss) return

        val loadedLivids = mc.theWorld.loadedEntityList.filter {
            it.name.contains("Livid") && it.name.length > 5 && it.name[1] == it.name[5]
        }

        if (loadedLivids.size <= 8) return

        lividTag = loadedLivids[0]
        livid = closestLivid(lividTag !!.name[5])
        if (livid != null) foundLivid = true

        if (thread != null && thread !!.isAlive) return


        thread = Thread({
                            Thread.sleep(1500)
                            val state = mc.theWorld.getBlockState(BlockPos(5, 109, 42))
                            val color = state.getValue(BlockStainedGlass.COLOR)
                            livid = closestLivid(
                                when (color) {
                                    EnumDyeColor.GREEN -> '2'
                                    EnumDyeColor.PURPLE -> '5'
                                    EnumDyeColor.GRAY -> '7'
                                    EnumDyeColor.BLUE -> '9'
                                    EnumDyeColor.LIME -> 'a'
                                    EnumDyeColor.MAGENTA -> 'd'
                                    EnumDyeColor.YELLOW -> 'e'
                                    EnumDyeColor.RED -> 'c'
                                    EnumDyeColor.WHITE -> 'f'
                                    else -> {
                                        modMessage("Error encountered during Livid Check with color:" + color.name)
                                        return@Thread
                                    }
                                }
                            ) ?: return@Thread
                        }, "[Livid ESP] Livid Check")

        thread !!.start()
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load?) {
        foundLivid = false
        livid = null
    }

    private fun closestLivid(chatFormatting: Char): Entity? {
        return mc.theWorld.loadedEntityList.filterIsInstance<EntityOtherPlayerMP>()
            .filter { it.name.equals(lividNames[chatFormatting]) }
            .sortedWith(Comparator.comparingDouble { lividTag !!.getDistanceSqToEntity(it) }).firstOrNull()
    }
}
