package org.http4k.tracing

import org.http4k.events.Event

sealed interface TraceStep

sealed interface Trace : TraceStep {
    val origin: Actor
    val target: Actor
    val request: String
    val children: List<Trace>
}

data class StartInteraction(val origin: String, val interactionName: String) : TraceStep,
    Event

object StartRendering : TraceStep, Event
object StopRendering : TraceStep, Event

data class RequestResponse(
    override val origin: Actor,
    override val target: Actor,
    override val request: String,
    val response: String,
    override val children: List<Trace>
) : Trace, TraceStep

data class FireAndForget(
    override val origin: Actor,
    override val target: Actor,
    override val request: String,
    override val children: List<Trace>
) : Trace, TraceStep

data class BiDirectional(
    override val origin: Actor,
    override val target: Actor,
    override val request: String,
    override val children: List<Trace>
) : Trace, TraceStep
