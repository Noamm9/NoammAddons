package com.github.noamm9.interfaces;

public interface ICoordRememberingSlot {
    void noammaddons_rememberCoords();

    void noammaddons_restoreCoords();

    int noammaddons_getOriginalX();

    int noammaddons_getOriginalY();

    void noammaddons_setX(int x);

    void noammaddons_setY(int y);
}
