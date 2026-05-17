package com.github.noamm9.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface IAbstractContainerScreen {
    @Accessor("hoveredSlot")
    Slot getHoveredSlot();

    @Accessor("leftPos")
    int getLeftPos();

    @Accessor("topPos")
    int getTopPos();

    @Accessor("leftPos")
    void setLeftPos(int value);

    @Accessor("topPos")
    void setTopPos(int value);

    @Accessor("imageWidth")
    int getImageWidth();

    @Accessor("imageHeight")
    int getImageHeight();

    @Accessor("imageWidth")
    void setImageWidth(int value);

    @Accessor("imageHeight")
    void setImageHeight(int value);
}
