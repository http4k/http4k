package org.http4k.lens

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.datastar.DatastarEvent
import org.http4k.datastar.Fragment
import org.http4k.datastar.MergeMode
import org.http4k.datastar.MergeMode.morph
import org.http4k.datastar.Selector
import org.http4k.datastar.SettleDuration
import org.http4k.datastar.SettleDuration.Companion.DEFAULT
import org.http4k.urlDecoded
import org.http4k.urlEncoded

// Present on all Datastar requests
val Header.DATASTAR_REQUEST get() = Header.boolean().defaulted("DATASTAR_REQUEST", false)

// Special content type used for datastar events
val Header.DATASTAR_CONTENT_TYPE
    get() = Header.map(::ContentType, ContentType::toHeaderValue).required("CONTENT_TYPE")

// Used for Datastae data when a request is a GET
val Query.DATASTAR_DATA get() = Query.map(String::urlDecoded, String::urlEncoded).required("datastar")

/**
 * Put datastar event into response as a datastar-merge-fragments event
 */
fun Response.datastar(
    vararg fragments: String,
    mergeMode: MergeMode = morph,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    settleDuration: SettleDuration? = DEFAULT,
    id: String? = null,
): Response = datastar(fragments.toList(), mergeMode, selector, useViewTransition, settleDuration, id)

/**
 * Put datastar event into response as a datastar-merge-fragments event
 */
@JvmName("datastarStrings")
fun Response.datastar(
    fragments: List<String>,
    mergeMode: MergeMode = morph,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    settleDuration: SettleDuration? = DEFAULT,
    id: String? = null,
): Response = datastar(fragments.map { Fragment.of(it) }, mergeMode, selector, useViewTransition, settleDuration, id)

/**
 * Put datastar event into response as a datastar-merge-fragments event
 */
fun Response.datastar(
    vararg fragments: Fragment,
    mergeMode: MergeMode = morph,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    settleDuration: SettleDuration? = DEFAULT,
    id: String? = null,
): Response = datastar(
    DatastarEvent.MergeFragments(
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
fun Response.datastar(
    fragments: List<Fragment>,
    mergeMode: MergeMode = morph,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    settleDuration: SettleDuration? = DEFAULT,
    id: String? = null,
): Response = datastar(
    DatastarEvent.MergeFragments(
        fragments.toList(),
        mergeMode,
        selector,
        useViewTransition,
        settleDuration,
        id
    )
)

/**
 * Inject a datastar event into a response
 */
fun Response.datastar(event: DatastarEvent) = with(Body.datastar().toLens() of event)

/**
 * Injects a datastar event into a response
 */
fun Body.Companion.datastar() = string(TEXT_EVENT_STREAM)
    .map<DatastarEvent>(
        { error("cannot parse event stream") },
        { it.toSseEvent().toMessage() }
    )
