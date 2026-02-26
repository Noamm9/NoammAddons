package com.github.noamm9.features.impl.dev

import com.github.noamm9.NoammAddons
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.utils.DataDownloader
import com.github.noamm9.utils.network.ProfileUtils
import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.vertex.PoseStack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.world.entity.Avatar
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.absoluteValue

object Cosmetics: Feature(toggled = true) {
    val customNames by ToggleSetting("Show Custom Names")
    val customSizes by ToggleSetting("Show Custom Sizes")

    val cosmeticPeople by lazy {
        DataDownloader.loadJson<Map<UUID, CosmeticData>>("cosmeticPeople.json").also {
            scope.launch {
                for ((uuid, cosmetic) in it.filter { it.value.hasCustomName }) {
                    val profile = ProfileUtils.getNameByUUID(uuid.toString()).getOrThrow()
                    TextReplacer.replaceMap[profile.name] = cosmetic.name
                    delay(200)
                }
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
    val GAME_PROFILE_KEY: RenderStateDataKey<GameProfile> = RenderStateDataKey.create { "${NoammAddons.MOD_ID}:game_profile" }

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