package com.github.noamm9.mixin;

import com.github.noamm9.NoammAddons;
import com.github.noamm9.features.impl.dev.cosmetics.badges.BadgeFontInjector;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(FontManager.class)
public class MixinFontManager {
    @Inject(method = "reload", at = @At("RETURN"), cancellable = true)
    private void reapplyBadges(PreparableReloadListener.SharedState currentReload, Executor taskExecutor, PreparableReloadListener.PreparationBarrier preparationBarrier, Executor reloadExecutor, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        cir.setReturnValue(cir.getReturnValue().whenCompleteAsync((_, error) -> {
            if (error == null) BadgeFontInjector.reapply();
        }, NoammAddons.getMc()));
    }
}