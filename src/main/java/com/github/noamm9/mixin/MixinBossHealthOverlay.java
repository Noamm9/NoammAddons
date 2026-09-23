package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.dungeon.BossBarHealth;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BossHealthOverlay.class)
public class MixinBossHealthOverlay {
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LerpingBossEvent;getName()Lnet/minecraft/network/chat/Component;"))
    public Component onRender(LerpingBossEvent instance, Operation<Component> original) {
        return BossBarHealth.onRender(instance, original);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;extractBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;)V"))
    public void onRenderbar(BossHealthOverlay instance, GuiGraphicsExtractor graphics, int x, int y, BossEvent event, Operation<Void> original) {
        if (BossBarHealth.INSTANCE.enabled && BossBarHealth.getHideBossBar().getValue()) return;
        original.call(instance, graphics, x, y, event);
    }
}