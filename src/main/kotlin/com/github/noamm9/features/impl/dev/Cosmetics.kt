package com.github.noamm9.features.impl.dev

import com.github.noamm9.NoammAddons
import com.github.noamm9.config.types.ButtonSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.dev.cosmetics.CosmeticData
import com.github.noamm9.features.impl.dev.text.TextReplacer
import com.github.noamm9.ui.notification.NotificationManager
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.MathUtils.vec
import com.github.noamm9.utils.NumbersUtils
import com.github.noamm9.utils.network.ProfileUtils
import com.github.noamm9.utils.network.WebUtils
import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.vertex.PoseStack
import kotlinx.coroutines.*
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.world.entity.Avatar
import java.util.*
import java.util.concurrent.*
import kotlin.math.abs

object Cosmetics: Feature(toggled = true) {
    val customNames by ToggleSetting("Show Custom Names", true)
    val customSizes by ToggleSetting("Show Custom Sizes", true)
    val showHalo by ToggleSetting("Show Halos", true)
    val reload by ButtonSetting("Reload Cosmetics") {
        if (System.currentTimeMillis() - lastReload >= 15_000) init()
        else NotificationManager.push("Cosmetics", "Please wait another ${NumbersUtils.formatTime(150_000 - (System.currentTimeMillis() - lastReload))} before reloading again.")
    }

    private lateinit var cosmeticPeople: Map<UUID, CosmeticData>
    private val profileNames = ConcurrentHashMap<UUID, String>()
    private var lastReload = System.currentTimeMillis()

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
    }

    @JvmStatic
    fun extractRenderStateHook(avatar: Avatar, state: AvatarRenderState) {
        if (! enabled) return
        if (avatar !is AbstractClientPlayer) return
        state.setData(GAME_PROFILE_KEY, avatar.gameProfile)
        state.setData(SNEAKING_KEY, avatar.isCrouching)
    }

    @JvmStatic
    fun scaleHook(state: AvatarRenderState, poseStack: PoseStack) {
        val gameProfile = state.getData(GAME_PROFILE_KEY) ?: return
        val data = cosmeticDataFor(gameProfile.id) ?: return

        if (customSizes.value && data.hasCustomSize) {
            if (data.sizeY < 0) poseStack.translate(0f, data.sizeY * 2f, 0f)
            poseStack.scale(data.sizeX, data.sizeY, data.sizeZ)
        }

        state.nameTagAttachment?.let { pos ->
            var scaleY = 1f
            var offset = 0f

            if (customSizes.value && data.hasCustomSize) scaleY = abs(data.sizeY)
            if (showHalo.value && data.hasHalo) offset += 0.15f

            state.nameTagAttachment = vec(pos.x, (pos.y + 0.15) * scaleY + offset, pos.z)
        }
    }

    fun cosmeticDataFor(uuid: UUID) = if (::cosmeticPeople.isInitialized) cosmeticPeople[uuid] else null
    val GAME_PROFILE_KEY = RenderStateDataKey.create<GameProfile> { "${NoammAddons.MOD_ID}:game_profile" }
    val SNEAKING_KEY = RenderStateDataKey.create<Boolean> { "${NoammAddons.MOD_ID}:sneaking" }
}