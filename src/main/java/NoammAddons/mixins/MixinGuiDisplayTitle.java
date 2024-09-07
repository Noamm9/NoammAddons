package NoammAddons.mixins;

import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import NoammAddons.events.RenderTitleEvent;


@Mixin(GuiIngameForge.class)
public class MixinGuiDisplayTitle {
    @Inject(method = "renderTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;pushMatrix()V", ordinal = 0), cancellable = true)
    private void onRenderTitle(int width, int height, float partialTicks, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new RenderTitleEvent())) {
            ci.cancel();
        }
    }
}