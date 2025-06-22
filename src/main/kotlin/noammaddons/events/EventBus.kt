package noammaddons.events

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.CopyOnWriteArrayList

object EventBus {
    val listeners = CopyOnWriteArrayList<EventListener<*>>()

    inline fun <reified T: Event> onEvent(noinline cb: (T) -> Unit): EventListener<out Event> {
        val listener = listeners.find { it.type == T::class.java && it.cb == cb }
            ?: EventListener(T::class.java, cb)
        listeners.add(listener)
        return listener
    }

    @SubscribeEvent
    fun onEvent(event: Event) {
        for (listener in listeners) {
            listener.invoke(event)
        }
    }

    init {
        MinecraftForge.EVENT_BUS.register(this)
    }
}