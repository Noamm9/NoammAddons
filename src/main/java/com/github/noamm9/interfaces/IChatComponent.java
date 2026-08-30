package com.github.noamm9.interfaces;

import net.minecraft.client.multiplayer.chat.GuiMessage;

import java.util.List;

public interface IChatComponent {
    List<GuiMessage.Line> getVisibleMessages();

    List<GuiMessage> getAllMessages();

    double getLineIndex();

    void refreshChat();
}
