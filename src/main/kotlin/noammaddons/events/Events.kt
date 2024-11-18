package noammaddons.events

import net.minecraft.block.state.IBlockState
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.model.ModelBase
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityItem
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.network.Packet
import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.util.BlockPos
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event


abstract class GuiContainerEvent(val container: Container, val gui: GuiContainer): Event() {
    @Cancelable
    class DrawSlotEvent(container: Container, gui: GuiContainer, var slot: Slot):
        GuiContainerEvent(container, gui)

    @Cancelable
    class SlotClickEvent(container: Container, gui: GuiContainer, var slot: Slot?, var slotId: Int):
        GuiContainerEvent(container, gui)

    class CloseEvent(container: Container, gui: GuiContainer): GuiContainerEvent(container, gui)

    @Cancelable
    class GuiMouseClickEvent(val mouseX: Int, val mouseY: Int, val button: Int, val guiScreen: GuiScreen): Event()
}

abstract class ClickEvent: Event() {
    @Cancelable
    class LeftClickEvent: ClickEvent()

    @Cancelable
    class RightClickEvent: ClickEvent()
}


abstract class PacketEvent: Event() {
    @Cancelable
    class Received(val packet: Packet<*>): PacketEvent()

    @Cancelable
    class Sent(val packet: Packet<*>): PacketEvent()
}


class RenderEntityModelEvent(
    var entity: EntityLivingBase,
    var p_77036_2_: Float,
    var p_77036_3_: Float,
    var p_77036_4_: Float,
    var p_77036_5_: Float,
    var p_77036_6_: Float,
    var scaleFactor: Float,
    var modelBase: ModelBase
): Event()


@Cancelable
class RenderTitleEvent(val title: String, val subTitle: String): Event()


@Cancelable
class MessageSentEvent(var message: String): Event()


@Cancelable
class RenderScoreBoardEvent(val objective: ScoreObjective, val scaledRes: ScaledResolution): Event()

@Cancelable
class BlockChangeEvent(val pos: BlockPos, val state: IBlockState): Event()

class RenderOverlay: Event()

class RenderWorld(val partialTicks: Float): Event()

@Cancelable
class Chat(var component: IChatComponent): Event()


@Cancelable
class Actionbar(val component: IChatComponent): Event()


class ServerTick: Event()

class Tick: Event()

class PreKeyInputEvent(val key: Int, val character: Char): Event()

@Cancelable
class renderPlayerlist(val width: Int, val scoreObjectiveIn: ScoreObjective?): Event()

abstract class RenderItemEntityEvent(
    val entity: EntityItem,
    val x: Double,
    val y: Double,
    val z: Double,
    val entityYaw: Float,
    val partialTicks: Float,
): Event() {

    @Cancelable
    class Pre(
        entity: EntityItem,
        x: Double,
        y: Double,
        z: Double,
        entityYaw: Float,
        partialTicks: Float
    ): RenderItemEntityEvent(entity, x, y, z, entityYaw, partialTicks)

    class Post(
        entity: EntityItem,
        x: Double,
        y: Double,
        z: Double,
        entityYaw: Float,
        partialTicks: Float,
    ): RenderItemEntityEvent(entity, x, y, z, entityYaw, partialTicks)
}

abstract class RenderChestEvent(var chest: TileEntityChest, var x: Double, var y: Double, var z: Double, var partialTicks: Float): Event() {

    class Pre(tileEntity: TileEntityChest, x: Double, y: Double, z: Double, partialTicks: Float): RenderChestEvent(tileEntity, x, y, z, partialTicks)

    class Post(tileEntity: TileEntityChest, x: Double, y: Double, z: Double, partialTicks: Float): RenderChestEvent(tileEntity, x, y, z, partialTicks)
}