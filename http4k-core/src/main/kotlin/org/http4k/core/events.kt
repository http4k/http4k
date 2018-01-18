package org.http4k.core

interface Event {
    val category: EventCategory

    companion object {
        data class Error(val message: String, val cause: Throwable? = null) : Event {
            override val category = EventCategory("error")
        }
    }
}

interface Events {
    fun raise(event: Event)
}

data class EventCategory(private val name: String) {
    override fun toString(): String = name
}