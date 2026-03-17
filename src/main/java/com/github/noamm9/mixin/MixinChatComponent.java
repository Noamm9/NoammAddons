package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.general.Chat;
import com.github.noamm9.interfaces.IChatComponent;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static com.github.noamm9.NoammAddons.mc;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent implements IChatComponent {

    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;

    @Shadow private int chatScrollbarPos;
    @Shadow protected abstract boolean isChatHidden();
    @Shadow public abstract boolean isChatFocused();
    @Shadow protected abstract int getWidth();
    @Shadow protected abstract double getScale();
    @Shadow public abstract int getLinesPerPage();
    @Shadow protected abstract int getLineHeight();

    private double screenToChatX(double x) {
        return (x / getScale()) - 4.0;
    }

    private double screenToChatY(double y) {
        double scaledHeight = mc.getWindow().getGuiScaledHeight();
        double yFromBottom = scaledHeight - y - 40.0;
        return yFromBottom / (getScale() * getLineHeight());
    }

    private int getMessageLineIndexAt(double x, double y) {
        if (!isChatFocused() || isChatHidden()) return -1;
        if (x < -4.0) return -1;

        double maxX = Math.floor(getWidth() / getScale());
        if (x > maxX) return -1;

        int maxLines = Math.min(getLinesPerPage(), trimmedMessages.size());
        if (y >= 0 && y < maxLines) {
            int index = (int) Math.floor(y + chatScrollbarPos);
            if (index >= 0 && index < trimmedMessages.size()) return index;
        }

        return -1;
    }

    @Override
    public double getMouseXtoChatX() {
        return screenToChatX(mc.mouseHandler.getScaledXPos(mc.getWindow()));
    }

    @Override
    public double getMouseYtoChatY() {
        return screenToChatY(mc.mouseHandler.getScaledYPos(mc.getWindow()));
    }

    @Override
    public double getLineIndex(double x, double y) {
        return getMessageLineIndexAt(x, y);
    }

    @Override
    public List<GuiMessage.Line> getVisibleMessages() {
        return this.trimmedMessages;
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void clearMessages(Component chatComponent, CallbackInfo ci) {
        Chat.addMassageHook(chatComponent, ci);
    }
}
