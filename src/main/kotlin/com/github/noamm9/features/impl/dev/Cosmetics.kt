package com.github.noamm9.features.impl.dev

import com.github.noamm9.NoammAddons
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.dev.text.TextReplacer
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.notification.NotificationManager
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.NumbersUtils
import com.github.noamm9.utils.network.ProfileUtils
import com.github.noamm9.utils.network.WebUtils
import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.vertex.PoseStack
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.world.entity.Avatar
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.absoluteValue

object Cosmetics: Feature(toggled = true) {
    val customNames by ToggleSetting("Show Custom Names", true)
    val customSizes by ToggleSetting("Show Custom Sizes", true)
    val reload by ButtonSetting("Reload Cosmetics") {
        if (System.currentTimeMillis() - lastReload >= 15_000) init()
        else NotificationManager.push("Cosmetics", "Please wait another ${NumbersUtils.formatTime(150_000 - (System.currentTimeMillis() - lastReload))} before reloading again.")
    }

    private var lastReload = System.currentTimeMillis()
    lateinit var cosmeticPeople: Map<UUID, CosmeticData>

    override fun init() {
        scope.launch(WebUtils.networkDispatcher) {
            lastReload = System.currentTimeMillis()
            NoammAddons.logger.info("fetching cosmeticPeople")
            WebUtils.getAs<Map<String, CosmeticData>>("https://old-api.noamm.org/cosmeticPeople.json").onSuccess { data ->
                cosmeticPeople = data.mapKeys { UUID.fromString(it.key) }
                val customNames = HashMap<String, String>()

                cosmeticPeople.filter { it.value.hasCustomName }.forEach { (uuid, cosmetic) ->
                    val profile = ProfileUtils.getNameByUUID(uuid.toString()).getOrThrow()
                    customNames[profile.name] = cosmetic.name
                }

                TextReplacer.setCustomReplacements(customNames)
            }.onFailure { cause ->
                NoammAddons.logger.error("Failed to load cosmetic people", cause)
                ChatUtils.modMessage("&cFailed to load cosmetic people: ${cause.message}")
            }
        }
    }

    @JvmStatic
    fun extractRenderStateHook(avatar: Avatar, state: AvatarRenderState) {
        if (! enabled) return
        if (! customSizes.value) return
        if (avatar !is AbstractClientPlayer) return
        state.setData<GameProfile>(GAME_PROFILE_KEY, avatar.gameProfile)
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

    @JvmField
    val GAME_PROFILE_KEY = RenderStateDataKey.create<GameProfile> { "${NoammAddons.MOD_ID}:game_profile" }

    @Serializable
    data class CosmeticData(
        val name: String = "",
        val sizeX: Float = 1f,
        val sizeY: Float = 1f,
        val sizeZ: Float = 1f,
    ) {
        val hasCustomName: Boolean get() = name.isNotEmpty()
        val hasCustomSize: Boolean get() = sizeX != 1f || sizeY != 1f || sizeZ != 1f
    }
}
