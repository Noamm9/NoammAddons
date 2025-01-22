package noammaddons.events

import net.minecraft.block.state.IBlockState
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.model.ModelBase
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.network.Packet
import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.util.BlockPos
import net.minecraft.util.IChatComponent
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event


@Cancelable
class DrawSlotEvent(val container: Container, val gui: GuiContainer, var slot: Slot): Event()

@Cancelable
class SlotClickEvent(val container: Container, val gui: GuiContainer, val slot: Slot?, val slotId: Int): Event()

@Cancelable
class GuiMouseClickEvent(val mouseX: Int, val mouseY: Int, val button: Int, val gui: GuiScreen): Event()

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

class PostRenderEntityModelEvent(
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

abstract class RenderChestEvent(var chest: TileEntityChest, var x: Double, var y: Double, var z: Double, var partialTicks: Float): Event() {

    class Pre(tileEntity: TileEntityChest, x: Double, y: Double, z: Double, partialTicks: Float): RenderChestEvent(tileEntity, x, y, z, partialTicks)

    class Post(tileEntity: TileEntityChest, x: Double, y: Double, z: Double, partialTicks: Float): RenderChestEvent(tileEntity, x, y, z, partialTicks)
}

class Inventory(
    val title: String,
    val windowId: Int,
    val slotCount: Int,
    val items: MutableMap<Int, ItemStack> = mutableMapOf(),
)

class InventoryFullyOpenedEvent(
    val title: String,
    val windowId: Int,
    val slotCount: Int,
    val items: Map<Int, ItemStack>
): Event()

@Cancelable
class GuiCloseEvent(val closedGui: GuiScreen?, val newGui: GuiScreen?): Event()

class WorldLoadPostEvent: Event()

@Cancelable
class RenderEntityEvent(val entity: Entity, val x: Double, val y: Double, val z: Double, val partialTicks: Float): Event()

class PostRenderEntityEvent(val entity: Entity, val x: Double, val y: Double, val z: Double, val partialTicks: Float): Event()

@Cancelable
class SoundPlayEvent(val name: String, val vol: Float, val pitch: Float, val pos: Vec3): Event()

@Cancelable
class BossbarUpdateEvent(val bossName: String, val maxHealth: Float, val health: Float, val healthScale: Float, val healthPresent: Float): Event()


/*
@Cancelable
class EntityMetadataEvent(
    val entity: Int,
    val flag: Int? = null,
    val airSupply: Int? = null,
    val name: String? = null,
    val customNameVisible: Any? = null,
    val isSilent: Any? = null,
    val noGravity: Any? = null,
    val health: Float? = null,
    val potionEffectColor: Any? = null,
    val isInvisible: Any? = null,
    val arrowsStuck: Any? = null,
    val unknown: Map<Int, Any?> = emptyMap(), // Holds unmapped metadata
    val Data: Map<Int, Any?> = emptyMap()
): Event()*/