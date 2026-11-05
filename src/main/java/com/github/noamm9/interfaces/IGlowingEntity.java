package com.github.noamm9.interfaces;

import java.awt.*;

public interface IGlowingEntity {
    Color noammaddons$glowColor();

    void noammaddons$glowColor(Color color);

    boolean noammaddons$isGlowing();

    void noammaddons$isGlowing(boolean value);

    int noammaddons$glowCheckTick();

    void noammaddons$glowCheckTick(int tick);

    boolean noammaddons$glowCancelled();

    void noammaddons$glowCancelled(boolean value);
}