package org.http4k.server

import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.Invalid
import org.http4k.lens.LensExtractor
import org.http4k.lens.LensFailure
import org.http4k.lens.LensGet
import org.http4k.lens.LensInjector
import org.http4k.lens.LensSet
import org.http4k.lens.Meta
import org.http4k.lens.Missing
import org.http4k.lens.ParamMeta

open class WsLensSpec<out OUT>(internal val meta: Meta, internal val get: LensGet<WsMessage, OUT>) {
    /**
     * Create a lens for this Spec
     */
    open fun toLens(): WsLens<OUT> {
        return WsLens(meta, { get("")(it).firstOrNull() ?: throw LensFailure(Missing(meta)) })
    }

    /**
     * Create another WebsocketLensSpec which applies the uni-directional transformation to the result. Any resultant Lens can only be used to extract the final type from a WsMessage.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT): WsLensSpec<NEXT> = WsLensSpec(meta, get.map(nextIn))
}

/**
 * Represents a bi-directional extraction of an entity from a target Body, or an insertion into a target Body.
 */
open class BiDiWsLensSpec<OUT>(meta: Meta,
                               get: LensGet<WsMessage, OUT>,
                               private val set: LensSet<WsMessage, OUT>) : WsLensSpec<OUT>(meta, get) {

    /**
     * Create another BiDiWsLensSpec which applies the bi-directional transformations to the result. Any resultant Lens can be
     * used to extract or insert the final type from/into a WsMessage.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = BiDiWsLensSpec(meta, get.map(nextIn), set.map(nextOut))

    /**
     * Create a lens for this Spec
     */
    override fun toLens(): BiDiWsLens<OUT> {
        val getLens = get("")
        val setLens = set("")
        return BiDiWsLens(meta,
            { getLens(it).let { if (it.isEmpty()) throw LensFailure(Missing(meta)) else it.first() } },
            { out: OUT, target: WsMessage -> setLens(out?.let { listOf(it) } ?: emptyList(), target) }
        )
    }
}

open class WsLens<out FINAL>(val meta: Meta, private val getLens: (WsMessage) -> FINAL) : LensExtractor<WsMessage, FINAL> {
    override operator fun invoke(target: WsMessage): FINAL = try {
        getLens(target)
    } catch (e: LensFailure) {
        throw e
    } catch (e: Exception) {
        throw LensFailure(Invalid(meta), cause = e, target = target)
    }
}

/**
 * A BiDiWsLens provides the bi-directional extraction of an entity from a target body, or the insertion of an entity
 * into a target body.
 */
class BiDiWsLens<FINAL>(meta: Meta,
                        get: (WsMessage) -> FINAL,
                        private val setLens: (FINAL, WsMessage) -> WsMessage)
    : LensInjector<FINAL, WsMessage>, WsLens<FINAL>(meta, get) {

    @Suppress("UNCHECKED_CAST")
    override operator fun <R : WsMessage> invoke(value: FINAL, target: R): R = setLens(value, target) as R
}


object WebsocketMsg : BiDiLensSpec<WsMessage, String>("websocket", ParamMeta.StringParam,
    LensGet { _, target -> listOf(target.content) },
    LensSet { name, values, target -> values.fold(target, { m, next -> m.copy(content = next) }) }
)