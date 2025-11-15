package noammaddons.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import noammaddons.events.ClickEvent;
import noammaddons.events.GuiCloseEvent;
import noammaddons.events.PreKeyInputEvent;
import noammaddons.events.WorldLoadPostEvent;
import noammaddons.features.impl.misc.RatProtection;
import noammaddons.utils.DataDownloader;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.events.EventDispatcher.postAndCatch;


@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Mutable
    @Shadow
    @Final
    private static ResourceLocation locationMojangPng;

    @Shadow
    private int leftClickCounter;
    @Unique
    private GuiScreen noammAddons$previousGuiScreen = null;

    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    private void onLeftClick(CallbackInfo ci) {
        if (postAndCatch(new ClickEvent.LeftClickEvent())) {
            ci.cancel();
        }
    }

    @Inject(method = "clickMouse", at = @At("RETURN"))
    private void postLeftClick(CallbackInfo ci) {
        leftClickCounter = 0;
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"), cancellable = true)
    private void onRightClick(CallbackInfo ci) {
        if (postAndCatch(new ClickEvent.RightClickEvent())) {
            ci.cancel();
        }
    }

    @Inject(method = "displayGuiScreen", at = @At("HEAD"), cancellable = true)
    private void onDisplayGuiScreen(GuiScreen newGuiScreen, CallbackInfo ci) {
        if (noammAddons$previousGuiScreen != null) {
            if (postAndCatch(new GuiCloseEvent(noammAddons$previousGuiScreen, newGuiScreen))) {
                ci.cancel();
                return;
            }
        }

        noammAddons$previousGuiScreen = newGuiScreen;
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;)V", at = @At("TAIL"))
    private void onWorldLoadPost1(WorldClient worldClientIn, CallbackInfo ci) {
        postAndCatch(new WorldLoadPostEvent());
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("TAIL"))
    private void onWorldLoadPost2(WorldClient worldClientIn, String loadingMessage, CallbackInfo ci) {
        postAndCatch(new WorldLoadPostEvent());
    }

    @Inject(method = "drawSplashScreen", at = @At("HEAD"))
    public void modifyMojangLogo(TextureManager textureManagerInstance, CallbackInfo ci) {
        locationMojangPng = new ResourceLocation("noammaddons:menu/loadingScreen.png");
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;dispatchKeypresses()V"), cancellable = true)
    public void keyPresses(CallbackInfo ci) {
        int k = (Keyboard.getEventKey() == 0) ? (Keyboard.getEventCharacter() + 256) : Keyboard.getEventKey();
        char character = Keyboard.getEventCharacter();
        if (Keyboard.getEventKeyState()) {
            if (postAndCatch(new PreKeyInputEvent(k, character)))
                ci.cancel();
        }
    }

    @Inject(method = "startGame", at = @At("HEAD"))
    public void onStartGame(CallbackInfo ci) {
        DataDownloader.INSTANCE.downloadData();
        RatProtection.INSTANCE.install();
    }
}


