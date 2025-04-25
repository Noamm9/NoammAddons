package noammaddons.events

import net.minecraft.block.Block
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
import net.minecraft.util.*
import net.minecraft.world.World
import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event
import noammaddons.features.impl.dungeons.dmap.core.map.Room
import noammaddons.features.impl.dungeons.dmap.core.map.RoomState
import noammaddons.features.impl.misc.Cosmetics
import noammaddons.utils.DungeonUtils


@Cancelable
class DrawSlotEvent(val container: Container, val gui: GuiContainer, var slot: Slot): Event()

@Cancelable
class SlotClickEvent(val container: Container, val gui: GuiContainer, val slot: Slot?, val slotId: Int): Event()

@Cancelable
class GuiMouseClickEvent(val mouseX: Int, val mouseY: Int, val button: Int, val gui: GuiScreen): Event()

@Cancelable
class GuiKeybourdInputEvent(val keyChar: Char, val keyCode: Int, val gui: GuiScreen): Event()

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

abstract class PostPacketEvent: Event() {
    class Received(val packet: Packet<*>): PacketEvent()

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
): Event() {
    companion object {
        fun getDummy(entity: EntityLivingBase): PostRenderEntityModelEvent {
            return PostRenderEntityModelEvent(
                entity, 0f, 0f,
                0f, 0f, 0f, 0f,
                Cosmetics.DragonWings
            )
        }
    }
}

@Cancelable
class RenderTitleEvent(val title: String, val subTitle: String): Event()

@Cancelable
class MessageSentEvent(var message: String): Event()

@Cancelable
class RenderScoreBoardEvent(val objective: ScoreObjective, val scaledRes: ScaledResolution): Event()

@Cancelable
class BlockChangeEvent(val pos: BlockPos, val block: Block, val oldBlock: Block): Event()

class RenderOverlay(val partialTicks: Float): Event()

class RenderWorld(val partialTicks: Float): Event()

@Cancelable
class Chat(var component: IChatComponent): Event()

@Cancelable
class Actionbar(val component: IChatComponent): Event()

@Cancelable
class AddMessageToChatEvent(val component: IChatComponent, val chatLineId: Int): Event()

class ServerTick: Event()

class Tick: Event()

class PreKeyInputEvent(val key: Int, val character: Char): Event()

@Cancelable
class renderPlayerlist(val width: Int, val scoreObjectiveIn: ScoreObjective?): Event()

abstract class RenderChestEvent(var chest: TileEntityChest, var x: Double, var y: Double, var z: Double, var partialTicks: Float): Event() {

    class Pre(tileEntity: TileEntityChest, x: Double, y: Double, z: Double, partialTicks: Float): RenderChestEvent(tileEntity, x, y, z, partialTicks)

    class Post(tileEntity: TileEntityChest, x: Double, y: Double, z: Double, partialTicks: Float): RenderChestEvent(tileEntity, x, y, z, partialTicks)
}

class InventoryFullyOpenedEvent(
    val title: String,
    val windowId: Int,
    val slotCount: Int,
    val items: Map<Int, ItemStack?>
): Event()

@Cancelable
class GuiCloseEvent(val closedGui: GuiScreen?, val newGui: GuiScreen?): Event()

class WorldLoadPostEvent: Event()

@Cancelable
class RenderEntityEvent(val entity: Entity, val x: Double, val y: Double, val z: Double, val partialTicks: Float): Event()

class PostRenderEntityEvent(val entity: Entity, val x: Double, val y: Double, val z: Double, val partialTicks: Float): Event()

@Cancelable
class SoundPlayEvent(val name: String, val vol: Float, val pitch: Float, val pos: Vec3): Event()

abstract class BossbarUpdateEvent(val bossName: String, val maxHealth: Float, val health: Float, val healthScale: Float, val healthPresent: Float): Event() {
    @Cancelable
    class Pre(bossName: String, maxHealth: Float, health: Float, healthScale: Float, healthPresent: Float): BossbarUpdateEvent(bossName, maxHealth, health, healthScale, healthPresent)

    class Post(bossName: String, maxHealth: Float, health: Float, healthScale: Float, healthPresent: Float): BossbarUpdateEvent(bossName, maxHealth, health, healthScale, healthPresent)
}

class PostEntityMetadataEvent(val entity: Entity): Event()


abstract class DungeonEvent: Event() {
    abstract class PuzzleEvent(val pazzle: String): DungeonEvent() {
        class Reset(pazzle: String): PuzzleEvent(pazzle)
        class Discovered(pazzle: String): PuzzleEvent(pazzle)
        class Completed(pazzle: String): PuzzleEvent(pazzle)
        class Failed(pazzle: String): PuzzleEvent(pazzle)
    }

    abstract class RoomEvent(val room: Room): DungeonEvent() {
        class onEnter(room: Room): RoomEvent(room)
        class onExit(room: Room): RoomEvent(room)

        class onStateChange(room: Room, val oldState: RoomState, val newState: RoomState, val roomPlayers: List<DungeonUtils.DungeonPlayer>): RoomEvent(room)
    }

    class SecretEvent(val type: SecretType, val pos: BlockPos): DungeonEvent() {
        enum class SecretType { CHEST, SKULL, ITEM, BAT, LAVER }
    }

    class PlayerDeathEvent(val name: String, val reason: String): DungeonEvent()

    class RunStatedEvent: DungeonEvent()
    class RunEndedEvent: DungeonEvent()
}

class WorldUnloadEvent: Event()

class EntityLeaveWorldEvent(val entity: Entity, val world: World): Event()