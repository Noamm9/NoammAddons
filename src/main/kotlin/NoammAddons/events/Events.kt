package NoammAddons.events

import net.minecraft.block.state.IBlockState
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.model.ModelBase
import net.minecraft.entity.EntityLivingBase
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.network.Packet
import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.util.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event

open class ClickEvent : Event() {
    @Cancelable
    class LeftClickEvent : ClickEvent()

    @Cancelable
    class RightClickEvent : ClickEvent()
}

open class GuiContainerEvent(val container: Container, val gui: GuiContainer) : Event() {
    @Cancelable
    class DrawSlotEvent(container: Container, gui: GuiContainer, var slot: Slot) :
        GuiContainerEvent(container, gui)

    @Cancelable
    class SlotClickEvent(container: Container, gui: GuiContainer, var slot: Slot?, var slotId: Int) :
        GuiContainerEvent(container, gui)

    class CloseEvent(container: Container, gui: GuiContainer) : GuiContainerEvent(container, gui)

    @Cancelable
    class GuiMouseClickEvent(val mouseX: Int, val mouseY: Int, val button: Int, val guiScreen: GuiScreen) : Event()

}

open class MovementUpdateEvent : Event() {
    @Cancelable
    class Pre : MovementUpdateEvent()

    @Cancelable
    class Post : MovementUpdateEvent()
}


open class PacketEvent : Event() {
    @Cancelable
    class Received(val packet: Packet<*>) : PacketEvent()

    @Cancelable
    class Sent(val packet: Packet<*>) : PacketEvent()
}


@Cancelable
class RenderLivingEntityEvent(
    var entity: EntityLivingBase,
    var p_77036_2_: Float,
    var p_77036_3_: Float,
    var p_77036_4_: Float,
    var p_77036_5_: Float,
    var p_77036_6_: Float,
    var scaleFactor: Float,
    var modelBase: ModelBase
) : Event()


@Cancelable
class RenderTitleEvent : Event()

@Cancelable
class MessageSentEvent(var message: String) : Event()

@Cancelable
class RenderScoreBoardEvent(val objective: ScoreObjective, val scaledRes: ScaledResolution) : Event()

@Cancelable
class BlockChangeEvent(val pos: BlockPos, val BlockState: IBlockState, val state: IBlockState, val worldObj: World) : Event()

class test(var pt: Int): Event()