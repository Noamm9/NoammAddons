package com.github.noamm9.mixin;

import com.github.noamm9.ui.customgui.ICoordRememberingSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Adapted from Firmament's OriginalSlotCoords.java
 * Source: https://github.com/nea89o/Firmament/blob/master/src/main/java/moe/nea/firmament/mixins/customgui/OriginalSlotCoords.java
 */
@Mixin(Slot.class)
public class MixinSlot implements ICoordRememberingSlot {
    @Shadow public int x;
    @Shadow public int y;

    @Unique public int originalX;
    @Unique public int originalY;

    @Override public void noammaddons_rememberCoords() { this.originalX = this.x; this.originalY = this.y; }
    @Override public void noammaddons_restoreCoords() { this.x = this.originalX; this.y = this.originalY; }
    @Override public int noammaddons_getOriginalX() { return originalX; }
    @Override public int noammaddons_getOriginalY() { return originalY; }
    @Override public void noammaddons_setX(int x) { this.x = x; }
    @Override public void noammaddons_setY(int y) { this.y = y; }
}
