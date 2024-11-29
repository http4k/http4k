package org.http4k.sse

import org.http4k.datastar.DatastarEvent
import org.http4k.datastar.DatastarEvent.MergeFragments
import org.http4k.datastar.DatastarEvent.MergeSignals
import org.http4k.datastar.DatastarEvent.RemoveFragments
import org.http4k.datastar.DatastarEvent.RemoveSignals
import org.http4k.datastar.Fragment
import org.http4k.datastar.MergeMode
import org.http4k.datastar.MergeMode.morph
import org.http4k.datastar.Script
import org.http4k.datastar.Selector
import org.http4k.datastar.SettleDuration
import org.http4k.datastar.SettleDuration.Companion.DEFAULT
import org.http4k.datastar.Signal
import org.http4k.datastar.SignalPath

fun Sse.sendMergeFragments(
    vararg fragments: Fragment,
    mergeMode: MergeMode = morph,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    settleDuration: SettleDuration? = DEFAULT,
    id: String? = null,
) = sendMergeFragments(fragments.toList(), mergeMode, selector, useViewTransition, settleDuration, id)

fun Sse.sendMergeFragments(
    fragments: List<Fragment>,
    mergeMode: MergeMode = morph,
    selector: Selector? = null,
    useViewTransition: Boolean = false,
    settleDuration: SettleDuration? = DEFAULT,
    id: String? = null,
) = send(
    MergeFragments(fragments, mergeMode, selector, useViewTransition, settleDuration, id).toSseEvent()
)

fun Sse.sendMergeSignals(vararg signals: Signal, onlyIfMissing: Boolean? = false, id: String? = null) =
    sendMergeSignals(signals.toList(), onlyIfMissing, id)

fun Sse.sendMergeSignals(signals: List<Signal>, onlyIfMissing: Boolean? = false, id: String? = null) =
    send(MergeSignals(signals, onlyIfMissing, id).toSseEvent())

fun Sse.sendRemoveSignals(vararg paths: SignalPath, id: String? = null) = sendRemoveSignals(paths.toList(), id)
fun Sse.sendRemoveSignals(paths: List<SignalPath>, id: String? = null) = send(RemoveSignals(paths, id).toSseEvent())

fun Sse.sendRemoveFragments(selector: Selector, id: String? = null) = send(RemoveFragments(selector, id).toSseEvent())

fun Sse.sendExecuteScript(
    script: Script, autoRemove: Boolean = true, attributes: List<Pair<String, String>> = emptyList(),
) = send(DatastarEvent.ExecuteScript(script, autoRemove, attributes).toSseEvent())
