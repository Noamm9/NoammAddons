package com.github.noamm9.interfaces;

/**
 * Adapted from Firmament's OriginalSlotCoords.java
 * Source: https://github.com/nea89o/Firmament/blob/master/src/main/java/moe/nea/firmament/mixins/customgui/OriginalSlotCoords.java
 */
public interface ICoordRememberingSlot {
    void noammaddons_rememberCoords();

    void noammaddons_restoreCoords();

    void noammaddons_setX(int x);

    void noammaddons_setY(int y);
}
