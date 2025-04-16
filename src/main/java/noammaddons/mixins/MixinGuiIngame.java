package noammaddons.mixins;

import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreObjective;
import noammaddons.events.RenderScoreBoardEvent;
import noammaddons.features.misc.SmoothBossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.events.RegisterEvents.postAndCatch;
import static noammaddons.features.general.ShowItemRarity.onSlotDraw;
import static noammaddons.features.hud.PlayerHud.cancelActionBar;
import static noammaddons.features.hud.PlayerHud.modifyText;
import static noammaddons.features.hud.SecretDisplay.removeSecrets;
import static noammaddons.noammaddons.config;

@Mixin(value = GuiIngame.class)
public class MixinGuiIngame {
    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    private void renderScoreboard(ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo ci) {
        if (postAndCatch(new RenderScoreBoardEvent(objective, scaledRes))) {
            ci.cancel();
        }
        if (config.getCustomScoreboard() && !ci.isCancelled()) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "setRecordPlaying(Ljava/lang/String;Z)V", at = @At("HEAD"), argsOnly = true)
    private String modifyActionBar(String text) {
        String result = text;
        result = modifyText(result);
        result = removeSecrets(result);
        return result;
    }

    @Inject(method = "setRecordPlaying(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void onActionBar(String message, boolean isPlaying, CallbackInfo ci) {
        if (!cancelActionBar(message)) return;
        ci.cancel();
    }

    @Inject(method = "renderBossHealth", at = @At("HEAD"), cancellable = true)
    private void onRenderBossBar(CallbackInfo ci) {
        SmoothBossBar.renderCustomBossBar(ci);
    }


    @Inject(method = "renderHotbarItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderItemAndEffectIntoGUI(Lnet/minecraft/item/ItemStack;II)V"))
    private void renderRarityOnHotbar(int index, int xPos, int yPos, float partialTicks, EntityPlayer player, CallbackInfo ci) {
        ItemStack itemStack = player.inventory.mainInventory[index];
        onSlotDraw(itemStack, xPos, yPos);
    }
}

