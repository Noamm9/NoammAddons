package com.github.noamm9.mixin;

import com.github.noamm9.websocket.packets.S2CPacketChat;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatListener.class)
public class MixinChatListener {
    @Inject(
        method = "handleSystemMessage",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addServerSystemMessage(Lnet/minecraft/network/chat/Component;)V"),
        cancellable = true
    )
    private void hideSimulatedPartyChat(Component message, boolean remote, CallbackInfo ci) {
        if (remote && S2CPacketChat.isHiddenPartyMessage()) ci.cancel();
    }
}
