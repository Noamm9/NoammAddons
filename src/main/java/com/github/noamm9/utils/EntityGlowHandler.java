package com.github.noamm9.utils;

import com.github.noamm9.event.EventBus;
import com.github.noamm9.event.impl.CheckEntityGlowEvent;
import com.github.noamm9.interfaces.IGlowingEntity;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

//#if LEGIT
//$import static com.github.noamm9.NoammAddons.mc;
//#endif

public final class EntityGlowHandler {
    private EntityGlowHandler() {}

    public static @Nullable Boolean getOverride(Entity entity) {
        //#if LEGIT
        //$var player = mc.player;
        //$if (player == null) return null;
        //$if (!player.hasLineOfSight(entity)) return null;
        //$if (entity.isInvisibleTo(player)) return null;
        //#endif

        var event = new CheckEntityGlowEvent(entity);
        EventBus.post(event);

        if (event.isCanceled()) return false;

        var glow = (IGlowingEntity) entity;
        glow.noammaddons$isGlowing(event.getShouldGlow());
        glow.noammaddons$glowColor(event.getColor());

        return glow.noammaddons$isGlowing() ? Boolean.TRUE : null;
    }
}
