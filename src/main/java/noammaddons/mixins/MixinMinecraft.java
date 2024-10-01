package noammaddons.mixins;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import noammaddons.events.ClickEvent;
import org.lwjgl.opengl.Display;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.apache.commons.io.IOUtils;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import static noammaddons.noammaddons.MOD_ID;
import static noammaddons.noammaddons.MOD_VERSION;


@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    private void onLeftClick(CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new ClickEvent.LeftClickEvent())) ci.cancel();
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"), cancellable = true)
    private void onRightClick(CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new ClickEvent.RightClickEvent())) ci.cancel();
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
            icon16 = this.getClass().getResourceAsStream("/assets/"+MOD_ID+"/menu/icons/logo-64x.png");
            icon32 = this.getClass().getResourceAsStream("/assets/"+MOD_ID+"/menu/icons/logo-32x.png");

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

