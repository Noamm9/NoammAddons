package noammaddons.mixins;

import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.common.MinecraftForge;
import noammaddons.events.renderPlayerlist;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiPlayerTabOverlay.class)
public abstract class MixinGuiPlayerTabOverlay {
    @Inject(method = "renderPlayerlist", at = @At("HEAD"), cancellable = true)
    public void renderCustomPlayerlist(int width, Scoreboard scoreboardIn, ScoreObjective scoreObjectiveIn, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new renderPlayerlist(width, scoreObjectiveIn))) {
            ci.cancel();
        }
    }
}


