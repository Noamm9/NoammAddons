package noammaddons.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import noammaddons.config.Config;
import noammaddons.config.CustomMainMenu.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenu {
    @Inject(method = "initGui", at = @At("HEAD"))
    public void initGui(CallbackInfo ci) {
        if (Config.INSTANCE.getCustomMainMenu()) {
            Minecraft.getMinecraft().displayGuiScreen(new TitleScreen());
        }
    }
}