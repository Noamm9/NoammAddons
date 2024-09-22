package NoammAddons.mixins;

import NoammAddons.events.RenderScoreBoardEvent;
import NoammAddons.features.hud.PlayerHud;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiIngame.class)
public class MixinGuiIngame {

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    private void renderScoreboard(ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new RenderScoreBoardEvent(objective, scaledRes)))
            ci.cancel();
    }

    @ModifyVariable(method = "setRecordPlaying(Ljava/lang/String;Z)V", at = @At("HEAD"), argsOnly = true)
    private String modifyActionBar(String text) {
        return PlayerHud.INSTANCE.modifyText(text);
    }
}
