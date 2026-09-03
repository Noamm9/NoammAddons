package com.github.noamm9.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface IMinecraft {
    // used to bypass mods like no chat reports from
    // hooking the getter and returning EMPTY_KEY_MANAGER
    @Accessor("profileKeyPairManager")
    ProfileKeyPairManager getKeyPair();
}