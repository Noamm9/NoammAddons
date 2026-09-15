package com.github.noamm9.mixin;

import com.github.noamm9.interfaces.IAccountProfileKeyPairManager;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.multiplayer.AccountProfileKeyPairManager;
import net.minecraft.util.CryptException;
import net.minecraft.world.entity.player.ProfileKeyPair;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.util.Optional;

@Mixin(AccountProfileKeyPairManager.class)
public abstract class MixinAccountProfileKeyPairManager implements IAccountProfileKeyPairManager {
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private UserApiService userApiService;

    @Shadow @Nullable protected abstract ProfileKeyPair fetchProfileKeyPair(UserApiService userApiService) throws CryptException, IOException;
    @Shadow protected abstract void writeProfileKeyPair(@Nullable ProfileKeyPair profileKeyPair);

    @Override
    public Optional<ProfileKeyPair> fetchKeyPair() {
        try {
            if (userApiService == UserApiService.OFFLINE) throw new IOException("Offline mode auth is not supported");
            ProfileKeyPair fetchedKeyPair = fetchProfileKeyPair(userApiService);
            writeProfileKeyPair(fetchedKeyPair);
            return Optional.ofNullable(fetchedKeyPair);
        } catch (CryptException | MinecraftClientException | IOException var3) {
            LOGGER.error("Failed to retrieve profile key pair", (Throwable) var3);
            writeProfileKeyPair(null);
            return Optional.empty();
        }
    }
}