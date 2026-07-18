package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.visual.CustomHubMap;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapRenderer.class)
public abstract class MixinMapRenderer {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void applyCustomHubMap(MapId mapId, MapItemSavedData mapData, MapRenderState mapRenderState, CallbackInfo ci) {
        CustomHubMap.applyRenderState(mapId, mapRenderState);
    }
}
