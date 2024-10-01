package noammaddons.sounds

import noammaddons.noammaddons.Companion.MOD_ID
import noammaddons.noammaddons.Companion.mc
import net.minecraft.util.ResourceLocation


object notificationsound {
    fun play() {
        mc.thePlayer.playSound("$MOD_ID:notificationsound", 1f, 1f)
    }
}