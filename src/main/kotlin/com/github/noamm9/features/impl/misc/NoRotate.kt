package com.github.noamm9.features.impl.misc

//#if CHEAT

import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.ILocalPlayer
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.MultiCheckboxSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.showIf
import com.github.noamm9.ui.clickgui.components.withDescription
import com.github.noamm9.utils.ActionBarParser
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.MathUtils.destructured
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.items.EtherwarpHelper
import com.github.noamm9.utils.items.InstantTransmissionHelper
import com.github.noamm9.utils.items.ItemUtils.customData
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import com.github.noamm9.utils.network.PacketUtils.send
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import net.minecraft.client.Camera
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

object NoRotate: Feature("Prevents the server from snapping back your head when teleporting.") {
    private val tpItems by MultiCheckboxSetting("Teleport Items", mutableMapOf(
        Pair("Etherwarp", false),
        Pair("Instant Transmission", false),
        Pair("Wither Impact", false)
    ))

    val zeroPingCamera by MultiCheckboxSetting("Zero Ping Camera", mutableMapOf(
        Pair("Etherwarp", false),
        Pair("Instant Transmission", false),
        Pair("Wither Impact", false)
    )).withDescription("Instently sets your camera at the teleport position.")

    private val resyncTimeout by SliderSetting("Resync Timeout", 500, 300, 1000, 50).showIf { zeroPingCamera.value.values.any { it } }

    val pendingTeleports = mutableListOf<TeleportPrediction>()
    private var lastWitherImpact = System.currentTimeMillis()

    override fun init() {
        register<WorldChangeEvent> {
            pendingTeleports.clear()
            lastWitherImpact = System.currentTimeMillis()
        }

        register<PacketEvent.Sent> {
            val packet = event.packet as? ServerboundUseItemPacket ?: return@register
            if (LocationUtils.world.equalsOneOf(WorldType.Home, WorldType.Garden)) return@register
            if (LocationUtils.dungeonFloorNumber == 7 && LocationUtils.inBoss) return@register
            if (ActionBarParser.currentMana < ActionBarParser.maxMana * 0.1) return@register
            if (ScanUtils.currentRoom?.data?.name.equalsOneOf("New Trap", "Old Trap", "Teleport Maze", "Boulder")) return@register
            if (mc.player !!.isPassenger) return@register
            val tpInfo = getTeleportInfo(mc.player !!.getItemInHand(packet.hand)) ?: return@register

            when (tpInfo.type) {
                TeleportType.Etherwarp -> doZeroPingEtherwarp(tpInfo, packet.yRot, packet.xRot)
                TeleportType.InstantTransmission -> doZeroPingInstantTransmission(tpInfo, packet.yRot, packet.xRot)
                TeleportType.WitherImpact -> doZeroPingWitherImpact(tpInfo, packet.yRot, packet.xRot)
            }
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            val packet = event.packet as? ClientboundPlayerPositionPacket ?: return@register
            if (pendingTeleports.isEmpty()) return@register
            val prediction = pendingTeleports.removeFirst().position
            val change = packet.change().position()

            if (change != prediction) pendingTeleports.clear()
            else {
                val player = mc.player ?: return@register

                val old = PositionMoveRotation.of(player)
                val new = PositionMoveRotation.calculateAbsolute(old, packet.change, packet.relatives)

                player.setPos(new.position())
                player.deltaMovement = new.deltaMovement()

                val newOldPos = PositionMoveRotation.calculateAbsolute(
                    PositionMoveRotation(player.oldPosition(), Vec3.ZERO, player.yRotO, player.xRotO), packet.change(), packet.relatives()
                )

                player.xo = newOldPos.position().x.also { player.xOld = it }
                player.yo = newOldPos.position().y.also { player.yOld = it }
                player.zo = newOldPos.position().z.also { player.zOld = it }

                ServerboundAcceptTeleportationPacket(packet.id).send()
                ServerboundMovePlayerPacket.PosRot(player.x, player.y, player.z, new.yRot, new.xRot, false, false).send()

                (player as ILocalPlayer).setLastYaw(new.yRot)
                (player as ILocalPlayer).setLastPitch(new.xRot)

                event.isCanceled = true
            }
        }
    }

    @JvmStatic
    fun cameraHook(instance: Camera, x: Double, y: Double, z: Double, original: Operation<Void>): Void? {
        if (! enabled) return original.call(instance, x, y, z)
        val player = mc.player ?: return original.call(instance, x, y, z)

        val config = zeroPingCamera.value.values.toList()
        val (x, y, z) = pendingTeleports.lastOrNull()?.takeIf { config[it.info.type.ordinal] }
            ?.position?.add(y = player.eyeHeight)?.destructured()
            ?: Triple(x, y, z)

        return original.call(instance, x, y, z)
    }

    private fun teleport(prediction: TeleportPrediction) {
        pendingTeleports.add(prediction)
        ThreadUtils.setTimeout(resyncTimeout.value) { pendingTeleports.remove(prediction) }
    }

    private fun doZeroPingEtherwarp(tpInfo: TeleportInfo, yaw: Float? = null, pitch: Float? = null) {
        val player = mc.player as ILocalPlayer

        val playerPos = pendingTeleports.lastOrNull()?.position ?: player.let { Vec3(it.serverX, it.serverY, it.serverZ) }
        val etherPos = EtherwarpHelper.getEtherPos(playerPos, MathUtils.getLookVec(yaw ?: player.serverYaw, pitch ?: player.serverPitch), tpInfo.distance)
        if (! etherPos.succeeded || etherPos.pos == null) return
        if (ScanUtils.getRoomFromPos(etherPos.vec !!)?.data?.name.equalsOneOf("Teleport Maze", "Boulder")) return

        val prediction = TeleportPrediction(etherPos.vec.add(0.5, 1.05, 0.5), tpInfo)
        teleport(prediction)
    }

    private fun doZeroPingInstantTransmission(tpInfo: TeleportInfo, yaw: Float? = null, pitch: Float? = null) {
        val player = mc.player as ILocalPlayer

        val playerPos = pendingTeleports.lastOrNull()?.position ?: Vec3(player.serverX, player.serverY, player.serverZ)
        val pos = InstantTransmissionHelper.predictTeleport(tpInfo.distance, playerPos, yaw ?: player.serverYaw, pitch ?: player.serverPitch) ?: return
        if (ScanUtils.getRoomFromPos(pos)?.data?.name.equalsOneOf("Teleport Maze", "Boulder")) return

        val prediction = TeleportPrediction(pos, tpInfo)
        teleport(prediction)
    }

    private fun doZeroPingWitherImpact(tpInfo: TeleportInfo, yaw: Float? = null, pitch: Float? = null) {
        if (System.currentTimeMillis() - lastWitherImpact <= 125) return // 8 CPS limit
        lastWitherImpact = System.currentTimeMillis()
        doZeroPingInstantTransmission(tpInfo, yaw, pitch)
    }

    private fun getTeleportInfo(stack: ItemStack?): TeleportInfo? {
        if (stack == null || stack.isEmpty) return null
        val player = mc.player as ILocalPlayer
        val sbId = stack.skyblockId
        val nbt = stack.customData

        if (sbId.equalsOneOf("ASPECT_OF_THE_VOID", "ASPECT_OF_THE_END")) {
            val tuners = nbt.getByte("tuned_transmission").orElse(0).toDouble()

            return if (tpItems.value["Etherwarp"] !! && player.isSneakingServer && nbt.getByte("ethermerge").orElse(0) == 1.toByte()) {
                TeleportInfo(57 + tuners, TeleportType.Etherwarp)
            }
            else if (tpItems.value["Instant Transmission"] !!) TeleportInfo(8 + tuners, TeleportType.InstantTransmission) else null

        }
        else if (tpItems.value["Wither Impact"] !! && nbt.getList("ability_scroll").toString().run {
                contains("SHADOW_WARP_SCROLL") && contains("IMPLOSION_SCROLL") && contains("WITHER_SHIELD_SCROLL")
            }) {
            return TeleportInfo(10.0, TeleportType.WitherImpact)
        }

        return null
    }

    enum class TeleportType { Etherwarp, InstantTransmission, WitherImpact }
    data class TeleportInfo(val distance: Double, val type: TeleportType)
    data class TeleportPrediction(val position: Vec3, val info: TeleportInfo)
}
//#endif