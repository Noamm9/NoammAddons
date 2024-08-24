package NoammAddons.utils

import NoammAddons.NoammAddons.Companion.mc
import net.minecraft.client.Minecraft

object PlayerUtils {
    fun closeScreen() {
        if (mc.currentScreen != null && mc.thePlayer != null) {
            mc.thePlayer.closeScreen()
        }
    }

    fun rightClick() {
        val method = try {
            Minecraft::class.java.getDeclaredMethod("func_147121_ag")
        } catch (e: NoSuchMethodException) {
            Minecraft::class.java.getDeclaredMethod("rightClickMouse")
        }
        method.isAccessible = true
        method.invoke(mc)
    }

    fun leftClick() {
        val method = try {
            Minecraft::class.java.getDeclaredMethod("func_147116_af")
        } catch (e: NoSuchMethodException) {
            Minecraft::class.java.getDeclaredMethod("clickMouse")
        }
        method.isAccessible = true
        method.invoke(mc)
    }
}