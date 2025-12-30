package noammaddons.utils.shaders

import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.shader.Framebuffer
import noammaddons.NoammAddons.Companion.mc
import noammaddons.events.RenderEntityModelEvent
import noammaddons.features.impl.esp.EspSettings.phase
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30.glBindFramebuffer
import org.lwjgl.opengl.GL30.glBlitFramebuffer
import java.awt.Color

object OutlineShader {
    private val shader = Shader(vertex = ShaderSource.VERTEX_SHADER, fragment = ShaderSource.FRAGMENT_SHADER)
    private var entityFbo: Framebuffer? = null
    private var hasAnyEntities = false
    private var isDepthBufferDirty = true

    fun reset() {
        isDepthBufferDirty = true
        hasAnyEntities = false
        if (entityFbo == null || entityFbo !!.framebufferWidth != mc.displayWidth || entityFbo !!.framebufferHeight != mc.displayHeight) {
            entityFbo?.deleteFramebuffer()
            entityFbo = Framebuffer(mc.displayWidth, mc.displayHeight, true)
        }

        val fbo = entityFbo ?: return
        fbo.framebufferClear()

        glBindFramebuffer(0x8D40, mc.framebuffer.framebufferObject)
    }

    fun renderSilhouette(event: RenderEntityModelEvent, color: Color) {
        val fbo = entityFbo ?: return

        if (isDepthBufferDirty) {
            val mainFbo = mc.framebuffer ?: return
            glBindFramebuffer(0x8CA8, mainFbo.framebufferObject)
            glBindFramebuffer(0x8CA9, fbo.framebufferObject)
            glBlitFramebuffer(
                0, 0, mainFbo.framebufferWidth, mainFbo.framebufferHeight,
                0, 0, fbo.framebufferWidth, fbo.framebufferHeight,
                GL_DEPTH_BUFFER_BIT, GL_NEAREST
            )
            isDepthBufferDirty = false
        }

        fbo.bindFramebuffer(false)
        hasAnyEntities = true

        glPushMatrix()
        glPushAttrib(GL_ALL_ATTRIB_BITS)

        glDisable(GL_ALPHA_TEST)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_LIGHTING)

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0f, 240.0f)

        if (phase) {
            glDisable(GL_DEPTH_TEST)
            glDepthMask(false)
        }
        else {
            glEnable(GL_DEPTH_TEST)
            glDepthMask(false)
        }

        glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)

        event.modelBase.render(
            event.entity, event.p_77036_2_, event.p_77036_3_,
            event.p_77036_4_, event.p_77036_5_, event.p_77036_6_, event.scaleFactor
        )

        glPopAttrib()
        glPopMatrix()

        mc.framebuffer.bindFramebuffer(false)
    }


    fun drawOutline(radius: Float) {
        if (! hasAnyEntities) return
        val fbo = entityFbo ?: return
        val sr = ScaledResolution(mc)

        glPushMatrix()
        glPushAttrib(GL_ALL_ATTRIB_BITS)

        glMatrixMode(GL_PROJECTION)
        glPushMatrix()
        glLoadIdentity()
        glOrtho(0.0, sr.scaledWidth_double, sr.scaledHeight_double, 0.0, - 1.0, 1.0)
        glMatrixMode(GL_MODELVIEW)
        glPushMatrix()
        glLoadIdentity()

        shader.bind()
        shader.setUniform("TexelSize", 1f / mc.displayWidth, 1f / mc.displayHeight)
        shader.setUniform("Radius", radius)

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glBindTexture(GL_TEXTURE_2D, fbo.framebufferTexture)
        val w = sr.scaledWidth_double
        val h = sr.scaledHeight_double
        glBegin(GL_QUADS)
        glTexCoord2f(0f, 1f)
        glVertex2d(0.0, 0.0)
        glTexCoord2f(0f, 0f)
        glVertex2d(0.0, h)
        glTexCoord2f(1f, 0f)
        glVertex2d(w, h)
        glTexCoord2f(1f, 1f)
        glVertex2d(w, 0.0)
        glEnd()

        shader.unbind()

        glMatrixMode(GL_PROJECTION)
        glPopMatrix()
        glMatrixMode(GL_MODELVIEW)
        glPopMatrix()
        glPopAttrib()
        glPopMatrix()

        hasAnyEntities = false
    }

    private object ShaderSource {
        const val VERTEX_SHADER = """
        #version 120
        void main() {
            gl_TexCoord[0] = gl_MultiTexCoord0;
            gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
        }
    """

        const val FRAGMENT_SHADER = """
        #version 120
        uniform sampler2D DiffuseSampler;
        uniform vec2 TexelSize;
        uniform float Radius;
        
        void main() {
            vec4 center = texture2D(DiffuseSampler, gl_TexCoord[0].st);
            if (center.a > 0.0) discard;
        
            for (float x = -Radius; x <= Radius; x++) {
                for (float y = -Radius; y <= Radius; y++) {
                    vec4 neighbor = texture2D(DiffuseSampler, gl_TexCoord[0].st + vec2(x, y) * TexelSize);
                    if (neighbor.a > 0.0) {
                        gl_FragColor = vec4(neighbor.rgb, 1.0); 
                        return;
                    }
                }
            }
            discard;
        }
    """
    }
}