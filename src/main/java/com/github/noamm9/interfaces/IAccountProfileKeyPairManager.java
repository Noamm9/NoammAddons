package com.github.noamm9.interfaces;

import net.minecraft.world.entity.player.ProfileKeyPair;

import java.util.Optional;

public interface IAccountProfileKeyPairManager {
    Optional<ProfileKeyPair> fetchKeyPair();
}