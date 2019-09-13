package org.http4k.events

data class TestEvent(val value: String = "hello") : Event {
    val category = EventCategory("Foo")
}