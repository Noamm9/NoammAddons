package com.github.noamm9.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface ILocalPlayer {
    @Accessor("xLast")
    double getServerX();

    @Accessor("yLast")
    double getServerY();

    @Accessor("zLast")
    double getServerZ();

    @Accessor("yRotLast")
    float getServerYaw();

    @Accessor("xRotLast")
    float getServerPitch();

    @Accessor("crouching")
    boolean isSneakingServer();

    @Accessor("wasSprinting")
    boolean isSprintingServer();

    @Accessor("lastOnGround")
    boolean onGroundServer();

    @Accessor("yRotLast")
    void setLastYaw(float yaw);

    @Accessor("xRotLast")
    void setLastPitch(float pitch);
}
