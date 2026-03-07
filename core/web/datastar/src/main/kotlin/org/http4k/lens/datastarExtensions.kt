package org.http4k.lens

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.datastar.DatastarEvent
import org.http4k.datastar.DatastarEvent.Companion.from
import org.http4k.datastar.DatastarEvent.PatchElements
import org.http4k.datastar.DatastarEvent.PatchSignals
import org.http4k.datastar.Element
import org.http4k.datastar.MorphMode
import org.http4k.datastar.MorphMode.outer
import org.http4k.datastar.MorphMode.outer
import org.http4k.datastar.Selector
import org.http4k.datastar.Signal
import org.http4k.sse.SseEventId
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Event
import org.http4k.urlDecoded
import org.http4k.urlEncoded

// Present on all Datastar requests
val Header.DATASTAR_REQUEST get() = Header.boolean().defaulted("datastar_request", false)

// Special content type used for datastar events
val Header.DATASTAR_CONTENT_TYPE
    get() = Header.map(::ContentType, ContentType::toHeaderValue).required("content_type")

// Used for Datastar model data when a request is a GET
val Query.DATASTAR_MODEL get() = Query.map(String::urlDecoded, String::urlEncoded).optional("datastar")

/**
 * Put datastar event into response as a datastar-patch-elements event
 */
fun Response.datastarElements(
    vararg elements: String,
    morphMode: MorphMode = outer,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    id: SseEventId? = null,
): Response = datastarElements(elements.toList(), morphMode, selector, useViewTransition, id)

/**
 * Put datastar event into response as a datastar-patch-elements event
 */
@JvmName("datastarElementsStrings")
fun Response.datastarElements(
    elements: List<String>,
    morphMode: MorphMode = outer,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    id: SseEventId? = null,
): Response =
    datastarElements(elements.map { Element.of(it) }, morphMode, selector, useViewTransition, id)

/**
 * Put datastar event into response as a datastar-patch-elements event
 */
fun Response.datastarElements(
    vararg elements: Element,
    morphMode: MorphMode = outer,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    id: SseEventId? = null,
): Response = datastarElements(
    PatchElements(
        elements.toList(),
        morphMode,
        selector,
        useViewTransition,
        id
    )
)

/**
 * Put datastar event into response as a datastar-patch-elements event
 */
@JvmName("datastarElements")
fun Response.datastarElements(
    elements: List<Element>,
    morphMode: MorphMode = outer,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    id: SseEventId? = null,
): Response = datastarElements(
    PatchElements(
        elements.toList(),
        morphMode,
        selector,
        useViewTransition,
        id
    )
)

/**
 * Inject a Datastar Event into a response. Appends the event to the existing body of the response
 */
fun Response.datastarElements(event: PatchElements) =
    contentType(TEXT_EVENT_STREAM).
    body(bodyString() + Response(OK).with(Body.datastarEvents().toLens() of listOf(event)).bodyString())

/**
 * Put datastar event into response as a datastar-patch-elements event
 */
fun Response.datastarSignals(vararg signals: Signal, onlyIfMissing: Boolean = false, id: SseEventId? = null) =
    datastarSignals(signals.toList(), onlyIfMissing, id)

/**
 * Put datastar event into response as a datastar-patch-elements event
 */
@JvmName("datastarSignals")
fun Response.datastarSignals(signals: List<Signal>, onlyIfMissing: Boolean = false, id: SseEventId? = null) =
    datastarSignals(PatchSignals(signals, onlyIfMissing, id))

/**
 * Inject a Datastar Event into a response. Appends the event to the existing body of the response
 */
fun Response.datastarSignals(event: PatchSignals) =
    contentType(TEXT_EVENT_STREAM).
    body(bodyString() + Response(OK).datastarEvents(listOf(event)).bodyString())

/**
 * Inject a Datastar PatchElements event into a Response as a Datastar event
 */
fun Response.html(vararg events: PatchElements) = html(events.flatMap { it.elements }.joinToString("\n") { it.value })

/**
 * Roundtrip datastar events
 */
fun Body.Companion.datastarEvents() = string(TEXT_EVENT_STREAM)
    .map({ it.toDatastarEvents() }, { it.joinToString("") { it.toSseEvent().toMessage() } })

fun Body.Companion.datastarElements(): BiDiBodyLensSpec<List<PatchElements>> =
    datastarEvents().map({ it.filterIsInstance<PatchElements>() }, { it })

fun Response.datastarEvents(events: List<DatastarEvent>) =
    contentType(TEXT_EVENT_STREAM).body(events.joinToString("") { it.toSseEvent().toMessage() })

/**
 * Extract the datastarEvents
 */
fun Response.datastarEvents() = bodyString().toDatastarEvents()

private fun String.toDatastarEvents() = split("\n\n")
    .filter(String::isNotEmpty)
    .map { SseMessage.parse(it) }
    .filterIsInstance<Event>()
    .map(::from)
