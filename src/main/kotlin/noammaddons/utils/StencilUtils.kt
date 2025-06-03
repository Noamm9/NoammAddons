package noammaddons.utils

import org.lwjgl.opengl.GL11

object StencilUtils {
    private var stencilLevel = 0

    fun beginStencilClip(drawMaskFunction: () -> Unit) {
        GL11.glEnable(GL11.GL_STENCIL_TEST)

        if (stencilLevel == 0) {
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT)
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF)
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INCR)
        }
        else {
            GL11.glStencilFunc(GL11.GL_EQUAL, stencilLevel, 0xFF)
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INCR)
        }

        GL11.glColorMask(false, false, false, false)
        GL11.glDepthMask(false)

        drawMaskFunction.invoke()

        GL11.glColorMask(true, true, true, true)
        GL11.glDepthMask(true)

        stencilLevel ++

        GL11.glStencilFunc(GL11.GL_EQUAL, stencilLevel, 0xFF)
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
    }

    fun endStencilClip() {
        if (stencilLevel <= 0) {
            GL11.glDisable(GL11.GL_STENCIL_TEST)
            return
        }

        stencilLevel --

        if (stencilLevel == 0) GL11.glDisable(GL11.GL_STENCIL_TEST)
        else {
            GL11.glStencilFunc(GL11.GL_EQUAL, stencilLevel, 0xFF)
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
        }
    }

    fun resetContext() {
        stencilLevel = 0
        if (GL11.glIsEnabled(GL11.GL_STENCIL_TEST)) {
            GL11.glDisable(GL11.GL_STENCIL_TEST)
        }
    }
}