package NoammAddons.utils

import net.minecraft.client.Minecraft

object Utils {
    fun Minecraft.getFPS() = Minecraft.getDebugFPS()
}