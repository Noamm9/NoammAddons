package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.dev.ModHider;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilScreen.class)
public class MixinAnvilScreen {
    @Redirect(method = "slotChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
    public String slotChanged$getString(Component instance) {
        //   if (!ModHider.INSTANCE.enabled) return instance.getString();
        String str = ModHider.getString(instance);
        //  if (!str.equals(instance.getString())) ModHider.addMod(str);
        return str;
    }

    @Redirect(method = "onNameChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
    public String onNameChanged$getString(Component instance) {
        //    if (!ModHider.INSTANCE.enabled) return instance.getString();
        String str = ModHider.getString(instance);
        // if (!str.equals(instance.getString())) ModHider.addMod(str);
        return str;
    }
}
