package org.reekwest.http.lens

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Status.Companion.NOT_ACCEPTABLE
import org.reekwest.http.core.copy
import org.reekwest.http.core.with
import org.reekwest.http.lens.Header.Common.CONTENT_TYPE
import java.nio.ByteBuffer
import java.util.Collections.emptyList

open class BodyLens<out FINAL>(val metas: List<Meta>, private val get: (HttpMessage) -> FINAL) : (HttpMessage) -> FINAL {

    @Throws(LensFailure::class)
    override operator fun invoke(target: HttpMessage): FINAL = try {
        get(target)
    } catch (e: LensFailure) {
        throw e
    } catch (e: Exception) {
        throw LensFailure(metas.map(::Invalid))
    }
}

class BiDiBodyLens<FINAL>(metas: List<Meta>,
                          get: (HttpMessage) -> FINAL,
                          private val set: (FINAL, HttpMessage) -> HttpMessage) : BodyLens<FINAL>(metas, get) {

    @Suppress("UNCHECKED_CAST")
    operator fun <R : HttpMessage> invoke(value: FINAL, target: R): R = set(value, target) as R

    infix fun <R : HttpMessage> to(value: FINAL): (R) -> R = { invoke(value, it) }
}


open class BodyLensSpec<MID, out OUT>(internal val metas: List<Meta>, internal val get: Get<HttpMessage, MID, OUT>) {
    open fun required(description: String? = null): BodyLens<OUT> {
        val getLens = get("")
        return BodyLens(metas, { getLens(it).firstOrNull() ?: throw LensFailure(metas.map(::Missing)) })
    }

    fun <NEXT> map(nextIn: (OUT) -> NEXT): BodyLensSpec<MID, NEXT> = BodyLensSpec(metas, get.map(nextIn))
}

open class BiDiBodyLensSpec<MID, OUT>(metas: List<Meta>,
                                      get: Get<HttpMessage, MID, OUT>,
                                      private val set: Set<HttpMessage, MID, OUT>) : BodyLensSpec<MID, OUT>(metas, get) {

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = BiDiBodyLensSpec(metas, get.map(nextIn), set.map(nextOut))

    override fun required(description: String?): BiDiBodyLens<OUT> {
        val getLens = get("")
        val setLens = set("")
        return BiDiBodyLens(metas,
            { getLens(it).let { if (it.isEmpty()) throw LensFailure(metas.map(::Missing)) else it.first() } },
            { out: OUT, target: HttpMessage -> setLens(out?.let { listOf(it) } ?: kotlin.collections.emptyList(), target) }
        )
    }
}

object Body {
    internal fun root(metas: List<Meta>, contentType: ContentType) = BiDiBodyLensSpec<ByteBuffer, ByteBuffer>(metas,
        Get { _, target ->
            if (CONTENT_TYPE(target) != contentType) throw LensFailure(CONTENT_TYPE.invalid(), status = NOT_ACCEPTABLE)
            target.body?.let { listOf(it) } ?: emptyList()
        },
        Set { _, values, target -> values.fold(target) { a, b -> a.copy(body = b) }.with(CONTENT_TYPE to contentType) }
    )

    fun string(contentType: ContentType, description: String? = null): BiDiBodyLensSpec<ByteBuffer, String>
        = root(listOf(Meta(true, "body", "body", description)), contentType).map(ByteBuffer::asString, String::asByteBuffer)

    fun binary(contentType: ContentType, description: String? = null): BiDiBodyLensSpec<ByteBuffer, ByteBuffer>
        = root(listOf(Meta(true, "body", "body", description)), contentType)
}

