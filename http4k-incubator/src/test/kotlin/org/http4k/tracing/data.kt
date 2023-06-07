package org.http4k.tracing

import org.http4k.tracing.ActorType.Database
import org.http4k.tracing.ActorType.Human
import org.http4k.tracing.ActorType.System

val c_to_external = RequestResponse(
    Actor("system-c", System),
    Actor("external", System),
    "c-to-external req",
    "200 c-to-external",
    listOf()
)

val bidi_b = BiDirectional(
    Actor("system.b", System),
    Actor("db", Database),
    "bidi-b req-resp",
    listOf()
)

val b_to_c = RequestResponse(
    Actor("system.b", System),
    Actor("system-c", System),
    "b-to-c req",
    "300 b-to-c",
    listOf(bidi_b, c_to_external)
)

val fireAndForget_user1_response = FireAndForget(
    Actor("events", System),
    Actor("user 1", Human),
    "event x",
    listOf()
)

val fireAndForget_user1 = FireAndForget(
    Actor("user 1", Human),
    Actor("events", System),
    "event a",
    listOf(fireAndForget_user1_response)
)

val entire_trace_1 = RequestResponse(
    Actor("user 1", Human),
    Actor("system.b", System),
    "init 1 req",
    "400 init 2",
    listOf(fireAndForget_user1, b_to_c)
)

val fireAndForget_d = FireAndForget(
    Actor("system/d", System),
    Actor("events", System),
    "event d",
    listOf()
)

val entire_trace_2 = RequestResponse(
    Actor("user 2", Human),
    Actor("system/d", System),
    "init 2 req",
    "500 init 2",
    listOf(fireAndForget_d)
)
