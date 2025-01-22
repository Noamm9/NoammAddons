package noammaddons.features.misc

import noammaddons.noammaddons.Companion.config
import org.lwjgl.Sys


/**
 *  @see noammaddons.mixins.MixinEntityPlayer
 */
class SmoothSneaking {
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
        val now = (Sys.getTime().toFloat() * 8) / Sys.getTimerResolution()
        val timeDiff = if (lastOperationTime == 0f) 0f else now - lastOperationTime
        lastOperationTime = now
        return timeDiff
    }

    private fun handleSneaking(timeDiff: Float): Float {
        return if (lastX < 1f) {
            lastX = (lastX + timeDiff).coerceAtMost(1f)
            getDownY(lastX)
        }
        else {
            finalizeSneaking()
        }
    }

    private fun handleUnsneaking(timeDiff: Float): Float {
        return if (lastX > 0) {
            lastX = (lastX - timeDiff).coerceAtLeast(0f)
            getUpY(lastX)
        }
        else {
            finalizeUnsneaking()
        }
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

    companion object {
        private val SNEAK_OFFSET get() = config.smoothSneakOffset

        private fun getUpY(x: Float): Float {
            return - SNEAK_OFFSET * x * x
        }

        private fun getDownY(x: Float): Float {
            val adjustedX = x - 1
            return SNEAK_OFFSET * adjustedX * adjustedX - SNEAK_OFFSET
        }
    }
}

