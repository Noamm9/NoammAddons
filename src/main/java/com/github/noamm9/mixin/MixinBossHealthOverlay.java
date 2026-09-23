package com.github.noamm9.mixin;


import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import com.github.noamm9.features.impl.visual.RenderOptimizer;
@Mixin(BossHealthOverlay.class)
public class MixinBossHealthOverlay {
    @ModifyConstant(method = "*", constant = @Constant(intValue = 182))
    private int injected(int value) {
        if(RenderOptimizer.INSTANCE.getHideBossBar().getValue() && RenderOptimizer.INSTANCE.enabled){
            return 0;
        }
        return value;
    }
}