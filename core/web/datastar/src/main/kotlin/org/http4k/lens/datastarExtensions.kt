package org.http4k.lens

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.datastar.DatastarEvent
import org.http4k.datastar.DatastarEvent.Companion.from
import org.http4k.datastar.DatastarEvent.MergeFragments
import org.http4k.datastar.Fragment
import org.http4k.datastar.MergeMode
import org.http4k.datastar.MergeMode.morph
import org.http4k.datastar.Selector
import org.http4k.datastar.SettleDuration
import org.http4k.datastar.SettleDuration.Companion.DEFAULT
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Event
import org.http4k.urlDecoded
import org.http4k.urlEncoded

// Present on all Datastar requests
val Header.DATASTAR_REQUEST get() = Header.boolean().defaulted("datastar_request", false)

// Special content type used for datastar events
val Header.DATASTAR_CONTENT_TYPE
    get() = Header.map(::ContentType, ContentType::toHeaderValue).required("content_type")

// Used for Datastae model data when a request is a GET
val Query.DATASTAR_MODEL get() = Query.map(String::urlDecoded, String::urlEncoded).optional("datastar")

/**
 * Put datastar event into response as a datastar-merge-fragments event
 */
fun Response.datastarFragments(
    vararg fragments: String,
    mergeMode: MergeMode = morph,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    settleDuration: SettleDuration? = DEFAULT,
    id: String? = null,
): Response = datastarFragments(fragments.toList(), mergeMode, selector, useViewTransition, settleDuration, id)

/**
 * Put datastar event into response as a datastar-merge-fragments event
 */
@JvmName("datastarFragmentsStrings")
fun Response.datastarFragments(
    fragments: List<String>,
    mergeMode: MergeMode = morph,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    settleDuration: SettleDuration? = DEFAULT,
    id: String? = null,
): Response =
    datastarFragments(fragments.map { Fragment.of(it) }, mergeMode, selector, useViewTransition, settleDuration, id)

/**
 * Put datastar event into response as a datastar-merge-fragments event
 */
fun Response.datastarFragments(
    vararg fragments: Fragment,
    mergeMode: MergeMode = morph,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    settleDuration: SettleDuration? = DEFAULT,
    id: String? = null,
): Response = datastarFragments(
    MergeFragments(
        fragments.toList(),
        mergeMode,
        selector,
        useViewTransition,
        settleDuration,
        id
    )
)

/**
 * Put datastar event into response as a datastar-merge-fragments event
 */
@JvmName("datastarFragments")
fun Response.datastarFragments(
    fragments: List<Fragment>,
    mergeMode: MergeMode = morph,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    settleDuration: SettleDuration? = DEFAULT,
    id: String? = null,
): Response = datastarFragments(
    MergeFragments(
        fragments.toList(),
        mergeMode,
        selector,
        useViewTransition,
        settleDuration,
        id
    )
)

/**
 * Inject a Datastar Event into a response
 */
fun Response.datastarFragments(event: MergeFragments) = with(Body.datastarEvents().toLens() of listOf(event))

/**
 * Inject a Datastar MergeFragments event into a Response as a Datastar event
 */
fun Response.html(vararg events: MergeFragments) = html(events.flatMap { it.fragments }.joinToString("\n") { it.value })

/**
 * Roundtrip datastar events
 */
fun Body.Companion.datastarEvents() = string(TEXT_EVENT_STREAM)
    .map({ it.toDatastarEvents() }, { it.joinToString("") { it.toSseEvent().toMessage() } })

fun Body.Companion.datastarFragments(): BiDiBodyLensSpec<List<MergeFragments>> =
    datastarEvents().map({ it.filterIsInstance<MergeFragments>() }, { it })

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
