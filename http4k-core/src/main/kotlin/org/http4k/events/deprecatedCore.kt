package org.http4k.events

@Deprecated("Renamed for clarity", ReplaceWith("and(next)"))
fun Events.then(next: Events) = and(next)
