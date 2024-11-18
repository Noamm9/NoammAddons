package noammaddons.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import noammaddons.events.ClickEvent;
import noammaddons.events.PreKeyInputEvent;
import org.apache.commons.io.IOUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static noammaddons.events.RegisterEvents.postAndCatch;
import static noammaddons.noammaddons.MOD_ID;
import static noammaddons.noammaddons.MOD_VERSION;


@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Mutable
    @Shadow
    @Final
    private static ResourceLocation locationMojangPng;

    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    private void onLeftClick(CallbackInfo ci) {
        if (postAndCatch(new ClickEvent.LeftClickEvent())) {
            ci.cancel();
        }
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"), cancellable = true)
    private void onRightClick(CallbackInfo ci) {
        if (postAndCatch(new ClickEvent.RightClickEvent())) {
            ci.cancel();
        }
    }

    @Inject(method = "createDisplay", at = @At("RETURN"))
    private void setWindowName(CallbackInfo ci) {
        Display.setTitle("NoammAddons - " + MOD_VERSION + "   ||   Noamm is the best!");
    }

    @Inject(method = "setWindowIcon", at = @At("HEAD"), cancellable = true)
    private void setCustomWindowIcon(CallbackInfo ci) {
        InputStream icon16 = null;
        InputStream icon32 = null;

        try {
            icon16 = this.getClass().getResourceAsStream("/assets/" + MOD_ID + "/menu/icons/logo-64x.png");
            icon32 = this.getClass().getResourceAsStream("/assets/" + MOD_ID + "/menu/icons/logo-32x.png");

            if (icon16 != null && icon32 != null) {
                ByteBuffer[] icons = new ByteBuffer[]{
                        noammAddons$readImageToBuffer(icon16),
                        noammAddons$readImageToBuffer(icon32)
                };
                Display.setIcon(icons);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(icon16);
            IOUtils.closeQuietly(icon32);
        }

        ci.cancel();
    }

    @Inject(method = "drawSplashScreen", at = @At("HEAD"))
    public void modifyMojangLogo(TextureManager textureManagerInstance, CallbackInfo ci) {
        locationMojangPng = new ResourceLocation("noammaddons:menu/loadingScreen.png");
    }

    @Inject(method = {"runTick"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;dispatchKeypresses()V")})
    public void keyPresses(CallbackInfo ci) {
        int k = (Keyboard.getEventKey() == 0) ? (Keyboard.getEventCharacter() + 256) : Keyboard.getEventKey();
        char character = Keyboard.getEventCharacter();
        if (Keyboard.getEventKeyState()) {
            postAndCatch(new PreKeyInputEvent(k, character));
        }
    }


    @Unique
    private ByteBuffer noammAddons$readImageToBuffer(InputStream imageStream) throws IOException {
        BufferedImage image = ImageIO.read(imageStream);
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());

        ByteBuffer buffer = ByteBuffer.allocate(4 * pixels.length);
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
            buffer.put((byte) (pixel & 0xFF));         // Blue
            buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
        }

        buffer.flip();
        return buffer;
    }
}


