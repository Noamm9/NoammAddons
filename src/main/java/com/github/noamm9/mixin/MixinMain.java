package com.github.noamm9.mixin;

import com.github.noamm9.init.DataDownloader;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MixinMain {
    @Inject(method = "main", at = @At("HEAD"), remap = false)
    private static void onGameLaunch(String[] args, CallbackInfo ci) {
        new Thread(DataDownloader::downloadData).start();
    }
}