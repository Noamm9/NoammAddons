package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.general.ChatFeatures;
import com.github.noamm9.interfaces.IChatComponent;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static com.github.noamm9.NoammAddons.mc;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent implements IChatComponent {
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;

    @Shadow public abstract boolean isChatFocused();
    @Shadow protected abstract int getWidth();
    @Shadow protected abstract double getScale();
    @Shadow public abstract int getLinesPerPage();
    @Shadow protected abstract int getLineHeight();

    @Inject(method = "addServerSystemMessage", at = @At("HEAD"), cancellable = true)
    private void clearMessages(Component message, CallbackInfo ci) {
        ChatFeatures.addMassageHook(message, ci);
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