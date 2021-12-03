package org.http4k.events

data class MyEvent(val value: String = "hello") : Event {
    val category = EventCategory("Foo")
}
