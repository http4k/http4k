package org.http4k.core

import org.http4k.events.then

@Deprecated("use org.http4k.events.Event", ReplaceWith("org.http4k.events.Event"))
typealias Event = org.http4k.events.Event

@Deprecated("use org.http4k.events.Events", ReplaceWith("org.http4k.events.Events"))
typealias Events = org.http4k.events.Events

@Deprecated("use org.http4k.events.EventCategory", ReplaceWith("org.http4k.events.EventCategory"))
typealias EventCategory = org.http4k.events.EventCategory

@Deprecated("use then()", ReplaceWith("this.then(next)"))
fun Events.then(next: Events): Events = then(next)
