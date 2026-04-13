package com.github.noamm9.interfaces;

import com.github.noamm9.ui.customgui.CustomGui;
import org.jetbrains.annotations.Nullable;

public interface IHasCustomGui {
    @Nullable
    CustomGui noammaddons_getCustomGui();

    void noammaddons_setCustomGui(@Nullable CustomGui gui);
}
