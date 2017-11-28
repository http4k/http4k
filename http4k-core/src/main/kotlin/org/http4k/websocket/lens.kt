package org.http4k.websocket

import org.http4k.asString
import org.http4k.core.Body
import org.http4k.lens.Invalid
import org.http4k.lens.LensExtractor
import org.http4k.lens.LensFailure
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.Meta
import org.http4k.lens.Missing
import org.http4k.lens.ParamMeta

internal val meta = Meta(true, "websocket", ParamMeta.ObjectParam, "")

open class WsMessageLensSpec<out OUT>(internal val get: LensGet<WsMessage, OUT>) {
    /**
     * Create a lens for this Spec
     */
    open fun toLens(): WsMessageLens<OUT> {
        return WsMessageLens({ get("")(it).firstOrNull() ?: throw LensFailure(Missing(meta)) })
    }

    /**
     * Create another WebsocketLensSpec which applies the uni-directional transformation to the result. Any resultant Lens can only be used to extract the final type from a WsMessage.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT): WsMessageLensSpec<NEXT> = WsMessageLensSpec(get.map(nextIn))
}

/**
 * Represents a bi-directional extraction of an entity from a target Body, or an insertion into a target Body.
 */
open class BiDiWsMessageLensSpec<OUT>(get: LensGet<WsMessage, OUT>,
                                      private val set: LensSet<WsMessage, OUT>) : WsMessageLensSpec<OUT>(get) {

    /**
     * Create another BiDiWsLensSpec which applies the bi-directional transformations to the result. Any resultant Lens can be
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
            { getLens(it).let { if (it.isEmpty()) throw LensFailure(Missing(meta)) else it.first() } },
            { out: OUT, target: WsMessage -> setLens(out?.let { listOf(it) } ?: emptyList(), target) }
        )
    }
}

open class WsMessageLens<out FINAL>(private val getLens: (WsMessage) -> FINAL) : LensExtractor<WsMessage, FINAL> {
    override operator fun invoke(target: WsMessage): FINAL = try {
        getLens(target)
    } catch (e: LensFailure) {
        throw e
    } catch (e: Exception) {
        throw LensFailure(Invalid(Meta(true, "websocket", ParamMeta.ObjectParam, "")), cause = e, target = target)
    }
}

/**
 * A BiDiWsLens provides the bi-directional extraction of an entity from a target body, or the insertion of an entity
 * into a target body.
 */
class BiDiWsMessageLens<FINAL>(get: (WsMessage) -> FINAL,
                               private val setLens: (FINAL, WsMessage) -> WsMessage)
    : WsMessageLens<FINAL>(get) {

    @Suppress("UNCHECKED_CAST")
    operator fun invoke(value: FINAL): WsMessage = setLens(value, WsMessage(Body.EMPTY))

    fun inject(value: FINAL) = invoke(value)
}

private val wsRoot =
    BiDiWsMessageLensSpec<Body>(
        LensGet { _, target -> listOf(target.body) },
        LensSet { _, values, target -> values.fold(target, { m, next -> m.body(next) }) })

fun WsMessage.Companion.binary() = wsRoot.map(Body::payload, { Body(it) })
fun WsMessage.Companion.string() = wsRoot.map({ it.payload.asString() }, { it: String -> Body(it) })
