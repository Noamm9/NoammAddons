package com.github.noamm9.features.impl.dev

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.dev.text.TextReplacer
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.notification.NotificationManager
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.NumbersUtils
import com.github.noamm9.utils.network.ProfileUtils
import com.github.noamm9.utils.network.WebUtils
import com.github.noamm9.utils.render.Render3D.renderBox
import com.github.noamm9.utils.render.RenderHelper.renderVec
import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.vertex.PoseStack
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.world.entity.Avatar
import net.minecraft.world.phys.Vec3
import java.awt.Color
import java.util.*
import java.util.concurrent.*
import kotlin.math.absoluteValue
import kotlin.math.sin

object Cosmetics: Feature(toggled = true) {
    val customNames by ToggleSetting("Show Custom Names", true)
    val customSizes by ToggleSetting("Show Custom Sizes", true)
    val showHalo by ToggleSetting("Show Halo", true).showIf { hasHaloAccess() }
    val reload by ButtonSetting("Reload Cosmetics") {
        if (System.currentTimeMillis() - lastReload >= 15_000) init()
        else NotificationManager.push("Cosmetics", "Please wait another ${NumbersUtils.formatTime(150_000 - (System.currentTimeMillis() - lastReload))} before reloading again.")
    }

    private var lastReload = System.currentTimeMillis()
    private val profileNames = ConcurrentHashMap<UUID, String>()
    lateinit var cosmeticPeople: Map<UUID, CosmeticData>

    // Ported 1:1 from the 1.8.9 halo.addBox(x, -10, z, 1, 1, 1) calls — pixel-unit (x, z) min corners.
    private val haloPixels = listOf(
        -2 to -6, 1 to 5, 0 to 5, -1 to 5, -2 to 5, -2 to 4, -3 to 4, -4 to 4, -4 to 3, -5 to 3,
        -5 to 2, -5 to 1, -6 to 1, -6 to -2, -6 to -1, 1 to 4, 2 to 4, 3 to 4, 3 to 3, 4 to 3,
        4 to 2, 4 to 1, 5 to 1, 5 to 0, 5 to -1, 5 to -2, -6 to 0, -5 to -2, -5 to -3, -5 to -4,
        -4 to -4, -4 to -5, -3 to -5, -2 to -5, 4 to -2, 4 to -3, 4 to -4, 3 to -4, 3 to -5,
        2 to -5, 1 to -5, 1 to -6, 0 to -6, -1 to -6
    )
    private const val PIXEL = 0.0625 // 1/16 block, matches the model's render(0.0625f) scale in 1.8.9

    //test since I cant edit api...
    private val testHaloPlayers = setOf(
        UUID.fromString("9806982a-1ecc-42bb-8899-cea654560bc3")
    )

    override fun init() {
        scope.launch(Dispatchers.IO) {
            lastReload = System.currentTimeMillis()
            NoammAddons.logger.info("fetching cosmeticPeople")
            WebUtils.getAs<Map<String, CosmeticData>>("https://api.noamm.org/cosmeticPeople.json").onSuccess { data ->
                cosmeticPeople = data.mapKeys { UUID.fromString(it.key) }
                coroutineScope {
                    val customNames = HashMap<String, String>()
                    val jobs = cosmeticPeople.filter { it.value.hasCustomName }.map { (uuid, cosmetic) ->
                        async {
                            val resolvedName = profileNames[uuid] ?: ProfileUtils.getNameByUUID(uuid).map { it.name }.getOrNull() ?: return@async
                            profileNames[uuid] = resolvedName
                            customNames[resolvedName] = cosmetic.name
                        }
                    }

                    jobs.awaitAll()
                    TextReplacer.init(customNames)
                }
            }.onFailure { cause ->
                NoammAddons.logger.error("Failed to load cosmetic people", cause)
                ChatUtils.modMessage("&cFailed to load cosmetic people: ${cause.message}")
            }
        }

        register<RenderWorldEvent> {
            if (! showHalo.value) return@register

            val bobOffset = (sin(System.currentTimeMillis() % 2000L / 2000f * (Math.PI.toFloat() * 2f)) + 1f) / 2f * 0.08f

            val iterator = level.entitiesForRendering().iterator()
            while (iterator.hasNext()) {
                val entity = iterator.next()
                if (entity !is AbstractClientPlayer) continue
                if (entity == mc.player && mc.options.cameraType.isFirstPerson) continue

                val data = if (::cosmeticPeople.isInitialized) cosmeticPeople[entity.gameProfile.id] else null
                if (data?.hasHalo != true && entity.gameProfile.id !in testHaloPlayers) continue

                val sizeScale = if (customSizes.value && data?.hasCustomSize == true) data.sizeY.absoluteValue else 1f
                val baseHeight = (entity.eyeHeight + 0.5f + bobOffset) * sizeScale
                val center = entity.renderVec.add(0.0, baseHeight.toDouble(), 0.0)
                val color = Color.decode(data?.haloColor ?: "#FFE650")

                val cubeSize = PIXEL * sizeScale
                for ((px, pz) in haloPixels) {
                    val x = center.x + (px + 0.5) * PIXEL * sizeScale
                    val z = center.z + (pz + 0.5) * PIXEL * sizeScale
                    event.ctx.renderBox(x, center.y, z, cubeSize, cubeSize, color, outline = false)
                }
            }
        }
    }

    @JvmStatic
    fun extractRenderStateHook(avatar: Avatar, state: AvatarRenderState) {
        if (! enabled) return
        if (! customSizes.value) return
        if (avatar !is AbstractClientPlayer) return
        state.setData(GAME_PROFILE_KEY, avatar.gameProfile)
    }

    @JvmStatic
    private fun hasHaloAccess(): Boolean {
        val uuid = mc.player?.uuid ?: return false
        val data = if (::cosmeticPeople.isInitialized) cosmeticPeople[uuid] else null
        return data?.hasHalo == true || uuid in testHaloPlayers
    }

    @JvmStatic
    fun scaleHook(state: AvatarRenderState, poseStack: PoseStack) {
        val gameProfile = state.getData(GAME_PROFILE_KEY) ?: return
        if (! ::cosmeticPeople.isInitialized) return
        val data = cosmeticPeople[gameProfile.id] ?: return
        if (! data.hasCustomSize) return

        if (data.sizeY < 0) poseStack.translate(0f, data.sizeY * 2f, 0f)
        poseStack.scale(data.sizeX, data.sizeY, data.sizeZ)

        state.nameTagAttachment?.let { pos ->
            val adjustedY = (pos.y + 0.15) * (data.sizeY.absoluteValue)
            state.nameTagAttachment = Vec3(pos.x, adjustedY, pos.z)
        }
    }

    val GAME_PROFILE_KEY = RenderStateDataKey.create<GameProfile> { "${NoammAddons.MOD_ID}:game_profile" }

    @Serializable
    data class CosmeticData(
        val name: String = "",
        val sizeX: Float = 1f,
        val sizeY: Float = 1f,
        val sizeZ: Float = 1f,
        val hasHalo: Boolean = false,
        val haloColor: String = "#FFE650",
    ) {
        val hasCustomName: Boolean get() = name.isNotEmpty()
        val hasCustomSize: Boolean get() = sizeX != 1f || sizeY != 1f || sizeZ != 1f
    }
}