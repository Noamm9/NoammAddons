package noammaddons.mixins;

import net.minecraftforge.client.GuiIngameForge;
import noammaddons.events.RenderTitleEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.events.EventDispatcher.postAndCatch;

@Mixin(GuiIngameForge.class)
public abstract class MixinGuiIngameForge {
    @Unique
    public String noammAddons$title;

    @Unique
    public String noammAddons$subtitle;

    @Inject(method = "renderTitle",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GlStateManager;pushMatrix()V"), cancellable = true)
    private void onRenderTitle(CallbackInfo ci) {
        noammAddons$title = ((AccessorGuiIngame) this).getDisplayTitle();
        noammAddons$subtitle = ((AccessorGuiIngame) this).getDisplaySubTitle();

        if (postAndCatch(new RenderTitleEvent(noammAddons$title, noammAddons$subtitle))) {
            ci.cancel();
        }
    }


}
