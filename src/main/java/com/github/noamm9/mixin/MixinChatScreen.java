package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.general.Chat;
import com.github.noamm9.ui.utils.componnents.ChatSearchBox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen extends Screen {
    @Shadow protected EditBox input;

    @Unique private ChatSearchBox searchBox;

    protected MixinChatScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addSearchBox(CallbackInfo ci) {
        searchBox = new ChatSearchBox(4, this.height - 26, this.width - 4, 12);
        searchBox.setValue(Chat.getSearchQuery());
        searchBox.setStatus(Chat::searchStatus);
        searchBox.setResponder(Chat::setSearch);
        searchBox.setVisible(Chat.isSearchBarOpen());

        addRenderableWidget(searchBox);
    }

    @Inject(method = "setInitialFocus", at = @At("TAIL"))
    private void focusSearchBox(CallbackInfo ci) {
        if (searchBox != null && searchBox.isVisible()) setFocused(searchBox);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void searchKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (searchBox == null) return;

        if (event.hasControlDown() && event.key() == GLFW.GLFW_KEY_F) {
            if (! Chat.isSearchAvailable()) return;
            toggleSearchBox();
            cir.setReturnValue(true);
            return;
        }

        if (! searchBox.isVisible() || ! searchBox.isFocused()) return;

        if (event.isEscape()) {
            toggleSearchBox();
            cir.setReturnValue(true);
            return;
        }

        // Enter keeps the filter applied and hands the focus back to the chat input.
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            setFocused(input);
            cir.setReturnValue(true);
            return;
        }

        searchBox.keyPressed(event);
        cir.setReturnValue(true);
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void closeSearchBox(CallbackInfo ci) {
        Chat.closeSearchBar();
    }

    @Unique
    private void toggleSearchBox() {
        boolean open = Chat.toggleSearchBar();

        searchBox.setVisible(open);
        if (open) {
            searchBox.setValue(Chat.getSearchQuery());
            searchBox.moveCursorToEnd(false);
            setFocused(searchBox);
        }
        else {
            searchBox.setValue("");
            setFocused(input);
        }
    }
}
