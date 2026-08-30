package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.general.Chat;
import com.github.noamm9.interfaces.IChatComponent;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static com.github.noamm9.NoammAddons.mc;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent implements IChatComponent {
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow @Final private List<GuiMessage> allMessages;
    @Shadow private int chatScrollbarPos;

    @Shadow public abstract boolean isChatFocused();
    @Shadow public abstract void resetChatScroll();
    @Shadow protected abstract int getWidth();
    @Shadow protected abstract double getScale();
    @Shadow public abstract int getLinesPerPage();
    @Shadow protected abstract int getLineHeight();

    @Shadow private void refreshTrimmedMessages() {
        throw new AssertionError();
    }

    @Inject(method = "addServerSystemMessage", at = @At("HEAD"), cancellable = true)
    private void clearMessages(Component message, CallbackInfo ci) {
        Chat.addMassageHook(message, ci);
    }

    @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"), cancellable = true)
    private void chatSearchHook(GuiMessage message, CallbackInfo ci) {
        if (Chat.isHiddenBySearch(message)) ci.cancel();
    }

    @Inject(method = "clearMessages", at = @At("HEAD"))
    private void chatClearedHook(boolean clearHistory, CallbackInfo ci) {
        Chat.onChatCleared();
    }

    @ModifyConstant(method = "addMessageToQueue", constant = @Constant(intValue = 100))
    private int modifyMaxHistory(int constant) {
        return Chat.maxChatHistory(constant);
    }

    @ModifyConstant(method = "addMessageToDisplayQueue", constant = @Constant(intValue = 100))
    private int modifyMaxTrimmedHistory(int constant) {
        return Chat.maxChatHistory(constant);
    }

    @Override
    public List<GuiMessage> getAllMessages() {
        return this.allMessages;
    }

    @Override
    public void refreshChat() {
        resetChatScroll();
        refreshTrimmedMessages();
    }

    @Override
    public List<GuiMessage.Line> getVisibleMessages() {
        return this.trimmedMessages;
    }

    @Override
    public double getLineIndex() {
        if (! isChatFocused()) return - 1;
        var mx = screenToChatX(mc.mouseHandler.getScaledXPos(mc.getWindow()));
        var my = screenToChatY(mc.mouseHandler.getScaledYPos(mc.getWindow()));
        var maxX = Math.floor(getWidth() / getScale());

        if (mx < - 4.0) return - 1;
        if (mx > maxX) return - 1;

        var maxLines = Math.min(getLinesPerPage(), trimmedMessages.size());
        if (my >= 0 && my < maxLines) {
            int index = (int) Math.floor(my + chatScrollbarPos);
            if (index >= 0 && index < trimmedMessages.size()) return index;
        }

        return - 1;
    }

    @Unique
    private double screenToChatX(double x) {
        return (x / getScale()) - 4.0;
    }

    @Unique
    private double screenToChatY(double y) {
        double scaledHeight = mc.getWindow().getGuiScaledHeight();
        double yFromBottom = scaledHeight - y - 40.0;
        return yFromBottom / (getScale() * getLineHeight());
    }
}