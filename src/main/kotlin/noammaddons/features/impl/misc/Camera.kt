package noammaddons.features.impl.misc

import net.minecraft.block.material.Material
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.client.event.EntityViewRenderEvent.*
import net.minecraftforge.client.event.RenderBlockOverlayEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PreKeyInputEvent
import noammaddons.features.Feature
import noammaddons.mixins.accessor.EntityAccessor
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.PlayerUtils.isHoldingWitherImpact
import noammaddons.utils.RenderHelper.getPartialTicks


object Camera: Feature() {
    @JvmField
    val smoothSneak = ToggleSetting("Smooth Sneak")

    @JvmField
    val noNausea = ToggleSetting("Disable Nausea") // @see noammaddons.mixins.MixinEntityLivingBase

    private val customFov = ToggleSetting("Custom FOV")
    private val fov = SliderSetting("FOV", 60f, 150f, 1f, mc.gameSettings.fovSetting).addDependency(customFov)
    private val removeWaterFov = ToggleSetting("Remove Water FOV")
    private val removeSelfieCam = ToggleSetting("Remove Selfie Cam")
    private val onlyWithHype = ToggleSetting("Only With Hype?").addDependency(removeSelfieCam)

    private val noBlind = ToggleSetting("Disable Blindness")
    private val noPortal = ToggleSetting("Disable Portal Effect")
    private val noFaceBlock = ToggleSetting("Disable Face Block")

    @JvmField
    val noPushOutOfBlocks = ToggleSetting("Disable Block Push")

    override fun init() = addSettings(
        SeperatorSetting("Animations"),
        smoothSneak,
        SeperatorSetting("Clean View"),
        customFov, fov, removeWaterFov, noBlind,
        noNausea, noPortal, noFaceBlock, noPushOutOfBlocks
    )

    @SubscribeEvent
    fun onFOVModifier(event: FOVModifier) {
        if (! customFov.value) return
        if (event.block.material == Material.water) return
        mc.gameSettings.fovSetting = fov.value
    }

    @SubscribeEvent
    fun onFOVModifier2(event: FOVModifier) {
        if (! removeWaterFov.value) return
        if (event.block.material != Material.water) return
        event.fov = event.fov * 70F / 60F
    }

    @SubscribeEvent
    fun onRenderFog(event: FogDensity) {
        if (! noBlind.value) return
        event.density = 0f
        GlStateManager.setFogStart(998f)
        GlStateManager.setFogEnd(999f)
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onRenderPortal(event: RenderGameOverlayEvent.Pre) {
        if (! noPortal.value) return
        if (event.type != RenderGameOverlayEvent.ElementType.PORTAL) return
        event.isCanceled = true
        (mc.thePlayer as? EntityAccessor)?.takeIf { it.isInPortal }?.run {
            setInPortal(false)
        }
    }

    @SubscribeEvent
    fun onRenderBlockOverlayEvent(event: RenderBlockOverlayEvent) {
        event.isCanceled = noFaceBlock.value
    }

    @SubscribeEvent
    fun onKey(event: PreKeyInputEvent) {
        if (! removeSelfieCam.value) return
        if (mc.gameSettings.thirdPersonView != 2) return
        if (mc.gameSettings.keyBindTogglePerspective.keyCode != event.key) return
        if (! isHoldingWitherImpact() && onlyWithHype.value) return

        event.isCanceled = true
        mc.gameSettings.thirdPersonView = 0
    }

    /**
     *  @see noammaddons.mixins.MixinEntityPlayer
     */
    object SmoothSneak {
        private const val SNEAK_OFFSET = 0.08f
        private const val animationSpeed = 2.5f // Increase to slow down animation
        private var lastState = false
        private var isAnimationDone = false
        private var lastOperationTime = 0f
        private var lastX = 0f

        fun getSneakingHeightOffset(isSneaking: Boolean): Float {
            if (lastState == isSneaking && isAnimationDone) {
                return if (isSneaking) - SNEAK_OFFSET else 0f
            }

            updateState(isSneaking)
            val timeDiff = calculateTimeDiff()

            return if (isSneaking) handleSneaking(timeDiff) else handleUnsneaking(timeDiff)
        }

        private fun updateState(isSneaking: Boolean) {
            if (lastState != isSneaking) {
                lastState = isSneaking
                isAnimationDone = false
            }
        }

        private fun calculateTimeDiff(): Float {
            val now = mc.thePlayer.ticksExisted.toFloat() + getPartialTicks()
            val timeDiff = if (lastOperationTime == 0f) 0f else now - lastOperationTime
            lastOperationTime = now
            return timeDiff
        }

        private fun handleSneaking(timeDiff: Float): Float {
            return if (lastX < 1f) {
                lastX = (lastX + timeDiff / animationSpeed).coerceAtMost(1f)
                getDownY(lastX)
            }
            else finalizeSneaking()
        }

        private fun handleUnsneaking(timeDiff: Float): Float {
            return if (lastX > 0) {
                lastX = (lastX - timeDiff / animationSpeed).coerceAtLeast(0f)
                getUpY(lastX)
            }
            else finalizeUnsneaking()
        }

        private fun finalizeSneaking(): Float {
            lastX = 1f
            isAnimationDone = true
            lastOperationTime = 0f
            return - SNEAK_OFFSET
        }

        private fun finalizeUnsneaking(): Float {
            lastX = 0f
            isAnimationDone = true
            lastOperationTime = 0f
            return 0f
        }

        private fun getUpY(x: Float): Float = - SNEAK_OFFSET * x * x

        private fun getDownY(x: Float): Float {
            val adjustedX = x - 1
            return SNEAK_OFFSET * adjustedX * adjustedX - SNEAK_OFFSET
        }

        @JvmStatic
        fun getEyeHeightHook(player: EntityPlayer): Float {
            return when {
                player.isPlayerSleeping -> 0.2f
                smoothSneak.value -> getSneakingHeightOffset(player.isSneaking)
                else -> - 0.08f
            }
        }
    }
}
