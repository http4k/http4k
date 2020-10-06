package org.http4k.lens

import org.http4k.asString
import org.http4k.core.Body
import org.http4k.websocket.WsMessage

internal val meta = Meta(true, "websocket", ParamMeta.ObjectParam, "message")

/**
 * Represents a extraction of an entity from a target WsMessage.
 */
open class WsMessageLensSpec<out OUT>(internal val get: LensGet<WsMessage, OUT>) {
    /**
     * Create a lens for this Spec
     */
    open fun toLens(): WsMessageLens<OUT> = WsMessageLens {
        get("message")(it).firstOrNull() ?: throw LensFailure(Missing(meta), target = it)
    }

    /**
     * Create another WsMessageLensSpec which applies the uni-directional transformation to the result. Any resultant Lens can only be used to extract the final type from a WsMessage.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT): WsMessageLensSpec<NEXT> = WsMessageLensSpec(get.map(nextIn))
}

/**
 * Represents a bi-directional extraction of an entity from a target Body, or an insertion into a target WsMessage.
 */
open class BiDiWsMessageLensSpec<OUT>(
    get: LensGet<WsMessage, OUT>,
    private val set: LensSet<WsMessage, OUT>
) : WsMessageLensSpec<OUT>(get) {

    /**
     * Create another BiDiWsMessageLensSpec which applies the bi-directional transformations to the result. Any resultant Lens can be
     * used to extract or insert the final type from/into a WsMessage.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = BiDiWsMessageLensSpec(get.map(nextIn), set.map(nextOut))

    /**
     * Create a lens for this Spec
     */
    override fun toLens(): BiDiWsMessageLens<OUT> {
        val getLens = get("")
        val setLens = set("")
        return BiDiWsMessageLens(
            { getLens(it).let { if (it.isEmpty()) throw LensFailure(Missing(meta), target = it) else it.first() } },
            { out: OUT, target: WsMessage -> setLens(out?.let { listOf(it) } ?: emptyList(), target) }
        )
    }
}

/**
 * A WsMessageLens provides the extraction of an entity from a target WsMessage.
 */
open class WsMessageLens<out FINAL>(private val getLens: (WsMessage) -> FINAL) : LensExtractor<WsMessage, FINAL> {
    override operator fun invoke(target: WsMessage): FINAL = try {
        getLens(target)
    } catch (e: LensFailure) {
        throw e
    } catch (e: Exception) {
        throw LensFailure(Invalid(meta), cause = e, target = target)
    }
}

/**
 * A BiDiWsMessageLens provides the bi-directional extraction of an entity from a target body, or the insertion of an entity
 * into a target WsMessage.
 */
class BiDiWsMessageLens<FINAL>(
    get: (WsMessage) -> FINAL,
    private val setLens: (FINAL, WsMessage) -> WsMessage
) :
    WsMessageLens<FINAL>(get) {

    @Suppress("UNCHECKED_CAST")
    operator fun invoke(target: FINAL): WsMessage = setLens(target, WsMessage(Body.EMPTY))

    fun create(value: FINAL): WsMessage = invoke(value)
}

private val wsRoot =
    BiDiWsMessageLensSpec<Body>(
        LensGet { _, target -> listOf(target.body) },
        LensSet { _, values, target -> values.fold(target) { m, next -> m.body(next) } }
    )

fun WsMessage.Companion.binary() = wsRoot.map(Body::payload) { Body(it) }
fun WsMessage.Companion.string() = wsRoot.map({ it.payload.asString() }, { it: String -> Body(it) })
