package com.github.noamm9.event.impl

import com.github.noamm9.event.Event
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

class ContainerFullyOpenedEvent(
    val title: Component,
    val winId: Int,
    val slotCount: Int,
    val items: HashMap<Int, ItemStack>
): Event(false)