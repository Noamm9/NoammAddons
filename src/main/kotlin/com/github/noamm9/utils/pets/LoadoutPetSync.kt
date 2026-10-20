package com.github.noamm9.utils.pets

/** Coordinates a prediction with the server response, in either packet/chat order. */
internal class LoadoutPetSync<T: Any>(
    private val cached: (String) -> T?,
    private val save: (String, T) -> Unit,
    private val display: (T) -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private data class Request<T>(val key: String, val name: String, val until: Long, var confirmed: Boolean = false, var pet: T? = null)
    private var request: Request<T>? = null

    fun select(key: String, name: String) {
        request = Request(key, name, now() + 5000)
    }

    fun confirm(name: String) {
        val pending = pending() ?: return
        // An older confirmation must not attach a response to a newer click.
        if (pending.name != name) return
        pending.confirmed = true
        val actual = pending.pet
        if (actual != null) {
            save(pending.key, actual)
            request = null
        }
        // Never overwrite a packet received before chat with a stale prediction.
        (actual ?: cached(pending.key))?.let(display)
    }

    fun receive(pet: T) {
        display(pet)
        pending()?.let {
            it.pet = pet
            if (it.confirmed) {
                save(it.key, pet)
                request = null
            }
        }
    }

    fun reset() { request = null }

    private fun pending(): Request<T>? {
        if (request?.let { now() >= it.until } == true) request = null
        return request
    }
}
