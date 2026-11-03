package com.github.noamm9.mixin;

import com.github.noamm9.config.types.ToggleSetting;
import com.github.noamm9.features.impl.floor7.F7Titles;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

// this exists cuz blade does this bs: https://github.com/BladeMasterGabe/blade-addons/blob/ca40d031a42ca18a7f095b48187580f06586e467/src/main/java/blade/addon/utils/dungeon/Section.java#L181
@Pseudo
@Mixin(targets = "blade.addon.utils.dungeon.Section", remap = false)
public class MixinBladeAddons {
    @WrapWithCondition(method = "parseMessage", at = @At(value = "INVOKE", target = "Lblade/addon/utils/Misc;forceTitle(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;)V"), require = 0)
    private static boolean allowTerminalTitle(Component title, Component subtitle) {
        return ! F7Titles.INSTANCE.enabled || ! ((ToggleSetting) F7Titles.INSTANCE.getSettingByName("Terminal Titles")).getValue();
    }
}
