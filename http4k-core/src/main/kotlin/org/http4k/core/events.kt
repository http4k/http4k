package org.http4k.core

interface Event {
    val category: EventCategory

    companion object {
        data class Error(val message: String, val cause: Throwable? = null) : Event {
            override val category = EventCategory("error")
        }
    }
}

typealias Events = (Event) -> Unit

fun Events.then(next: Events): Events = { it.also(this).also(next) }

data class EventCategory(private val name: String) {
    override fun toString(): String = name
}