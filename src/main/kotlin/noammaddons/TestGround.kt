// this is where I checked stuff before making them into a feature
// apart from that useless file

package noammaddons

import net.minecraft.client.Minecraft
import net.minecraft.util.Vec3
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Vector4f
import java.nio.FloatBuffer
import kotlin.math.roundToInt





object TestGround {
	
	fun projectTo2D(x: Double, y: Double, z: Double): Pair<Int, Int>? {
		val modelViewMatrix = BufferUtils.createFloatBuffer(16)
		val projectionMatrix = BufferUtils.createFloatBuffer(16)
		
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewMatrix)
		GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionMatrix)
		
		val viewport = BufferUtils.createIntBuffer(16)
		GL11.glGetInteger(GL11.GL_VIEWPORT, viewport)
		
		val modelView = Matrix4f()
		val projection = Matrix4f()
		
		modelView.load(modelViewMatrix)
		projection.load(projectionMatrix)
		
		val worldPosition = Vector4f(x.toFloat(), y.toFloat(), z.toFloat(), 1.0f)
		
		Matrix4f.transform(modelView, worldPosition, worldPosition)
		
		if (worldPosition.z < 0.0f) return null
		
		Matrix4f.transform(projection, worldPosition, worldPosition)
		
		if (worldPosition.w != 0.0f) {
			worldPosition.x /= worldPosition.w
			worldPosition.y /= worldPosition.w
		} else {
			return null
		}
		
		if (worldPosition.x !in -1.0f..1.0f || worldPosition.y !in -1.0f..1.0f) {
			return null
		}
		
		val screenX = ((1.0f + worldPosition.x) * 0.5f * viewport.get(2)).roundToInt()
		val screenY = ((1.0f - worldPosition.y) * 0.5f * viewport.get(3)).roundToInt()
		
		return Pair(screenX, screenY)
	}
	
	fun projectEntityTo2D(entity: net.minecraft.entity.Entity): Pair<Int, Int>? {
		return projectTo2D(entity.posX, entity.posY + entity.eyeHeight.toDouble(), entity.posZ)
	}
	
	
}
