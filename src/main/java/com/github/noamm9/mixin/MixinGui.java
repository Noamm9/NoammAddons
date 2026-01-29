package com.github.noamm9.mixin;

import com.github.noamm9.event.EventBus;
import com.github.noamm9.event.impl.ActionBarMessageEvent;
import com.github.noamm9.event.impl.RenderOverlayEvent;
import com.github.noamm9.features.impl.general.FEAT_ItemRarity;
import com.github.noamm9.features.impl.tweaks.Camera;
import com.github.noamm9.features.impl.visual.DarkMode;
import com.github.noamm9.features.impl.visual.PlayerHud;
import com.github.noamm9.features.impl.visual.Scoreboard;
import com.github.noamm9.utils.ColorUtils;
import com.github.noamm9.utils.DebugHUD;
import com.github.noamm9.utils.location.LocationUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Objective;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(Gui.class)
public abstract class MixinGui {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow @Nullable private Component title;
    @Shadow @Nullable private Component subtitle;

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private static void renderArmor(GuiGraphics guiGraphics, Player player, int i, int j, int k, int l, CallbackInfo ci) {
        if (PlayerHud.INSTANCE.getHideArmorbar().getValue()) {
            ci.cancel();
        }
    }

    @Shadow
    public abstract Font getFont();

    @Inject(method = "renderTitle", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;scale(FF)Lorg/joml/Matrix3x2f;", ordinal = 0, shift = At.Shift.AFTER))
    private void onScaleTitle(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (title == null) return;

        float maxWidth = minecraft.getWindow().getGuiScaledWidth() * 0.85f;
        float currentWidth = minecraft.font.width(title) * 4.0f;
        if (currentWidth > maxWidth) {
            float scaleFactor = maxWidth / currentWidth;
            guiGraphics.pose().scale(scaleFactor);
        }
    }

    @Inject(method = "renderTitle", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;scale(FF)Lorg/joml/Matrix3x2f;", ordinal = 1, shift = At.Shift.AFTER))
    private void onScaleSubtitle(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (subtitle == null) return;

        float maxWidth = minecraft.getWindow().getGuiScaledWidth() * 0.85f;
        float currentWidth = minecraft.font.width(subtitle) * 2.0f;
        if (currentWidth > maxWidth) {
            float scaleFactor = maxWidth / currentWidth;
            guiGraphics.pose().scale(scaleFactor);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderSleepOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    public void onRenderHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (this.minecraft.options.hideGui) return;
        if (this.minecraft.getDebugOverlay().showDebugScreen()) return;
        EventBus.post(new RenderOverlayEvent(guiGraphics, deltaTracker));

        DebugHUD.render(guiGraphics);
    }

    @Inject(method = "render", at = @At(value = "TAIL"))
    public void onRenderHudPost(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (DarkMode.INSTANCE.enabled) guiGraphics.fill(
            0, 0,
            minecraft.getWindow().getGuiScaledWidth(),
            minecraft.getWindow().getGuiScaledHeight(),
            ColorUtils.INSTANCE.withAlpha(Color.BLACK, DarkMode.getOpacity()).getRGB()
        );
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderPortalOverlay(GuiGraphics guiGraphics, float f, CallbackInfo ci) {
        if (Camera.INSTANCE.enabled && Camera.getHidePortalOverlay().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderConfusionOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderConfusionOverlay(GuiGraphics guiGraphics, float f, CallbackInfo ci) {
        if (Camera.INSTANCE.enabled && Camera.getDisableNausea().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    public void renderScoreboardSidebar(GuiGraphics guiGraphics, Objective objective, CallbackInfo ci) {
        if (Scoreboard.INSTANCE.enabled) {
            ci.cancel();
        }
    }

    /*
    @Inject(method = "renderTabList", at = @At("HEAD"), cancellable = true)
    public void renderTabList(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ci.cancel();

        Scoreboard scoreboard = this.minecraft.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.LIST);
        if (!this.minecraft.options.keyPlayerList.isDown()
            || this.minecraft.isLocalServer() && this.minecraft.player.connection.getListedOnlinePlayers().size() <= 1 && objective == null) {
            this.tabList.setVisible(false);
        } else {
            this.tabList.setVisible(true);
            guiGraphics.nextStratum();
            ClientBranding.


            drawTablist(guiGraphics);
        }
    }

    */

    @ModifyVariable(method = "setOverlayMessage", at = @At("HEAD"), argsOnly = true)
    private Component onSetOverlayMessage(Component component) {
        var event = new ActionBarMessageEvent(component);
        if (EventBus.post(event)) return Component.empty();
        return Component.literal(event.getMessage());
    }

    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    public void renderPlayerHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (PlayerHud.INSTANCE.getHideHealthbar().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    public void renderFood(GuiGraphics guiGraphics, Player player, int i, int j, CallbackInfo ci) {
        if (PlayerHud.INSTANCE.getHideFoodbar().getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (LocationUtils.inSkyblock) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void onRenderHotbarSlot(GuiGraphics guiGraphics, int i, int j, DeltaTracker deltaTracker, Player player, ItemStack itemStack, int k, CallbackInfo ci) {
        if (!FEAT_ItemRarity.INSTANCE.enabled) return;
        if (FEAT_ItemRarity.INSTANCE.getDrawOnHotbar().getValue()) {
            FEAT_ItemRarity.onSlotDraw(guiGraphics, itemStack, i, j);
        }
    }
}