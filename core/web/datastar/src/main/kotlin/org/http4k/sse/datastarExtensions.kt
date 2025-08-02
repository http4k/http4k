package org.http4k.sse

import org.http4k.datastar.DatastarEvent.PatchElements
import org.http4k.datastar.DatastarEvent.PatchSignals
import org.http4k.datastar.Element
import org.http4k.datastar.MorphMode
import org.http4k.datastar.MorphMode.outer
import org.http4k.datastar.Selector
import org.http4k.datastar.Signal

fun Sse.sendPatchElements(
    vararg elements: Element,
    morphMode: MorphMode = outer,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    id: SseEventId? = null,
) = sendPatchElements(elements.toList(), morphMode, selector, useViewTransition, id)

fun Sse.sendPatchElements(
    elements: List<Element>,
    morphMode: MorphMode = outer,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    id: SseEventId? = null,
) = send(
    PatchElements(elements, morphMode, selector, useViewTransition, id).toSseEvent()
)

fun Sse.sendMergeSignals(vararg signals: Signal, onlyIfMissing: Boolean? = false, id: SseEventId? = null) =
    sendMergeSignals(signals.toList(), onlyIfMissing, id)

fun Sse.sendMergeSignals(signals: List<Signal>, onlyIfMissing: Boolean? = false, id: SseEventId? = null) =
    send(PatchSignals(signals, onlyIfMissing, id).toSseEvent())

