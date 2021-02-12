package org.http4k

import org.http4k.cloudevents.toCloudEventReader
import org.http4k.core.Method.POST
import org.http4k.core.Request

fun main() {
    val toEvent = Request(POST, "").toCloudEventReader().toEvent()
    println(toEvent)
}
