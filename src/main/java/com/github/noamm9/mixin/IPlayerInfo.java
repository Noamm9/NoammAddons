package com.github.noamm9.mixin;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerInfo.class)
public interface IPlayerInfo {
    // Bypasses getTabListDisplayName(), which Tiers (https://github.com/PvPTiers/Tiers) decorates, to expose the original server value
    @Accessor("tabListDisplayName")
    Component getRawTabListDisplayName();
}