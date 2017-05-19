package org.http4k.lens

import org.http4k.asByteBuffer
import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.core.Status.Companion.NOT_ACCEPTABLE
import org.http4k.core.with
import org.http4k.lens.ContentNegotiation.NonStrict
import org.http4k.lens.Header.Common.CONTENT_TYPE
import org.http4k.lens.ParamMeta.FileParam
import org.http4k.lens.ParamMeta.StringParam
import java.nio.ByteBuffer

/**
 * A BodyLens provides the uni-directional extraction of an entity from a target body.
 */
open class BodyLens<out FINAL>(val metas: List<Meta>, private val get: (HttpMessage) -> FINAL) : (HttpMessage) -> FINAL {

    /**
     * Lens operation to get the value from the target
     * @throws LensFailure if the value could not be retrieved from the target (missing/invalid etc)
     */
    @Throws(LensFailure::class)
    override operator fun invoke(target: HttpMessage): FINAL = try {
        get(target)
    } catch (e: LensFailure) {
        throw e
    } catch (e: Exception) {
        throw LensFailure(metas.map(::Invalid))
    }
}

/**
 * A BiDiBodyLens provides the bi-directional extraction of an entity from a target body, or the insertion of an entity
 * into a target body.
 */
class BiDiBodyLens<FINAL>(metas: List<Meta>,
                          get: (HttpMessage) -> FINAL,
                          private val set: (FINAL, HttpMessage) -> HttpMessage) : BodyLens<FINAL>(metas, get) {

    /**
     * Lens operation to set the value into the target
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <R : HttpMessage> invoke(value: FINAL, target: R): R = set(value, target) as R

    /**
     * Bind this Lens to a value, so we can set it into a target
     */
    infix fun <R : HttpMessage> of(value: FINAL): (R) -> R = { invoke(value, it) }
}

/**
 * Represents a uni-directional extraction of an entity from a target Body.
 */
open class BodyLensSpec<out OUT>(internal val metas: List<Meta>, internal val get: LensGet<HttpMessage, ByteBuffer, OUT>) {
    /**
     * Create a lens for this Spec
     */
    open fun toLens(): BodyLens<OUT> {
        val getLens = get("")
        return BodyLens(metas, { getLens(it).firstOrNull() ?: throw LensFailure(metas.map(::Missing)) })
    }

    /**
     * Create another BodyLensSpec which applies the uni-directional transformation to the result. Any resultant Lens can only be
     * used to extract the final type from a Body.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT): BodyLensSpec<NEXT> = BodyLensSpec(metas, get.map(nextIn))
}

/**
 * Represents a bi-directional extraction of an entity from a target Body, or an insertion into a target Body.
 */
open class BiDiBodyLensSpec<OUT>(metas: List<Meta>,
                                 get: LensGet<HttpMessage, ByteBuffer, OUT>,
                                 private val set: LensSet<HttpMessage, ByteBuffer, OUT>) : BodyLensSpec<OUT>(metas, get) {

    /**
     * Create another BiDiBodyLensSpec which applies the bi-directional transformations to the result. Any resultant Lens can be
     * used to extract or insert the final type from/into a Body.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = BiDiBodyLensSpec(metas, get.map(nextIn), set.map(nextOut))

    /**
     * Create a lens for this Spec
     */
    override fun toLens(): BiDiBodyLens<OUT> {
        val getLens = get("")
        val setLens = set("")
        return BiDiBodyLens(metas,
            { getLens(it).let { if (it.isEmpty()) throw LensFailure(metas.map(::Missing)) else it.first() } },
            { out: OUT, target: HttpMessage -> setLens(out?.let { listOf(it) } ?: kotlin.collections.emptyList(), target) }
        )
    }
}

internal fun root(metas: List<Meta>, acceptedContentType: ContentType, contentNegotiation: ContentNegotiation) = BiDiBodyLensSpec(metas,
    LensGet { _, target ->
        contentNegotiation(acceptedContentType, CONTENT_TYPE(target))
        target.body.let { listOf(it.payload) }
    },
    LensSet { _, values, target -> values.fold(target) { a, b -> a.body(Body(b)) }.with(CONTENT_TYPE of acceptedContentType) }
)

/**
 * Modes for determining if a passed content type is acceptable.
 */
enum class ContentNegotiation {
    Strict {
        override fun invoke(expected: ContentType, actual: ContentType?) {
            if (actual != expected) throw LensFailure(CONTENT_TYPE.invalid(), status = NOT_ACCEPTABLE)
        }
    },
    NonStrict {
        override fun invoke(expected: ContentType, actual: ContentType?) {
            if (actual != null && actual != expected) throw LensFailure(CONTENT_TYPE.invalid(), status = NOT_ACCEPTABLE)
        }
    };

    @Throws(LensFailure::class)
    abstract operator fun invoke(expected: ContentType, actual: ContentType?)
}

fun Body.Companion.string(contentType: ContentType, description: String? = null, contentNegotiation: ContentNegotiation = NonStrict): BiDiBodyLensSpec<String>
    = root(listOf(Meta(true, "body", StringParam, "body", description)), contentType, contentNegotiation).map(ByteBuffer::asString, String::asByteBuffer)

fun Body.Companion.binary(contentType: ContentType, description: String? = null, contentNegotiation: ContentNegotiation = NonStrict): BiDiBodyLensSpec<ByteBuffer>
    = root(listOf(Meta(true, "body", FileParam, "body", description)), contentType, contentNegotiation)
