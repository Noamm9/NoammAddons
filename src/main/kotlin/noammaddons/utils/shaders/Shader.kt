package noammaddons.utils.shaders

import org.lwjgl.opengl.GL20.*
import java.awt.Color

class Shader(vertex: String, fragment: String) {
    private val programId: Int = glCreateProgram()

    init {
        val vId = glCreateShader(GL_VERTEX_SHADER)
        glShaderSource(vId, vertex)
        glCompileShader(vId)

        val fId = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(fId, fragment)
        glCompileShader(fId)

        glAttachShader(programId, vId)
        glAttachShader(programId, fId)
        glLinkProgram(programId)
    }

    fun bind() = glUseProgram(programId)
    fun unbind() = glUseProgram(0)

    fun setUniform(name: String, x: Float, y: Float) = glUniform2f(glGetUniformLocation(programId, name), x, y)
    fun setUniform(name: String, f: Float) = glUniform1f(glGetUniformLocation(programId, name), f)
    fun setUniform(name: String, c: Color) = glUniform4f(glGetUniformLocation(programId, name), c.red / 255f, c.green / 255f, c.blue / 255f, c.alpha / 255f)
}