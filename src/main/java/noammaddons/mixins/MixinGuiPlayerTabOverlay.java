package noammaddons.mixins;

import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import noammaddons.events.RenderPlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.events.EventDispatcher.postAndCatch;

@Mixin(GuiPlayerTabOverlay.class)
public abstract class MixinGuiPlayerTabOverlay {
    @Inject(method = "renderPlayerlist", at = @At("HEAD"), cancellable = true)
    public void renderCustomPlayerlist(int width, Scoreboard scoreboardIn, ScoreObjective scoreObjectiveIn, CallbackInfo ci) {
        if (postAndCatch(new RenderPlayerList(width, scoreObjectiveIn))) {
            ci.cancel();
        }
    }
}


