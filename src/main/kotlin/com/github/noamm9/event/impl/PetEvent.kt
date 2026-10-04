package com.github.noamm9.event.impl

import com.github.noamm9.event.Event
import com.github.noamm9.utils.PetUtils.Pet

abstract class PetEvent(val pet: Pet?): Event(cancelable = false) {
    class Change(pet: Pet?, val cause: Cause): PetEvent(pet)
    class LevelUp(pet: Pet): PetEvent(pet)

    enum class Cause { SUMMON, DESPAWN, AUTOPET, MENU }
}