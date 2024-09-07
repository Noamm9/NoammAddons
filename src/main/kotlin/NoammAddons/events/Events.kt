package NoammAddons.events

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.model.ModelBase
import net.minecraft.entity.EntityLivingBase
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.network.Packet
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
    class GuiMouseClickEvent(val mouseX: Int, val mouseY: Int, val button: Int) : Event()
}

open class MovementUpdateEvent : Event() {
    @Cancelable
    class Pre : MovementUpdateEvent()

    @Cancelable
    class Post : MovementUpdateEvent()
}

@Cancelable
class ReceivePacketEvent(val packet: Packet<*>) : Event()

@Cancelable
class SentPacketEvent(val packet: Packet<*>) : Event()

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