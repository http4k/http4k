package org.http4k.lens

import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.core.with
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.ParamMeta.FileParam
import org.http4k.lens.ParamMeta.StringParam

/**
 * A BodyLens provides the uni-directional extraction of an entity from a target body.
 */
open class BodyLens<out FINAL>(val metas: List<Meta>, val contentType: ContentType, private val getLens: (HttpMessage) -> FINAL) : LensExtractor<HttpMessage, FINAL> {

    override operator fun invoke(target: HttpMessage): FINAL = try {
        getLens(target)
    } catch (e: LensFailure) {
        throw e
    } catch (e: Exception) {
        throw LensFailure(metas.map(::Invalid), cause = e, target = target)
    }
}

/**
 * A BiDiBodyLens provides the bi-directional extraction of an entity from a target body, or the insertion of an entity
 * into a target body.
 */
class BiDiBodyLens<FINAL>(metas: List<Meta>,
                          contentType: ContentType,
                          get: (HttpMessage) -> FINAL,
                          private val setLens: (FINAL, HttpMessage) -> HttpMessage)
    : LensInjector<FINAL, HttpMessage>, BodyLens<FINAL>(metas, contentType, get) {

    @Suppress("UNCHECKED_CAST")
    override operator fun <R : HttpMessage> invoke(value: FINAL, target: R): R = setLens(value, target) as R
}

/**
 * Represents a uni-directional extraction of an entity from a target Body.
 */
open class BodyLensSpec<out OUT>(internal val metas: List<Meta>, internal val contentType: ContentType, internal val get: LensGet<HttpMessage, OUT>) {
    /**
     * Create a lens for this Spec
     */
    open fun toLens(): BodyLens<OUT> = with(get("")) {
        BodyLens(metas, contentType) { this(it).firstOrNull() ?: throw LensFailure(metas.map(::Missing), target = it) }
    }

    /**
     * Create another BodyLensSpec which applies the uni-directional transformation to the result. Any resultant Lens can only be
     * used to extract the final type from a Body.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT): BodyLensSpec<NEXT> = BodyLensSpec(metas, contentType, get.map(nextIn))
}

/**
 * Represents a bi-directional extraction of an entity from a target Body, or an insertion into a target Body.
 */
open class BiDiBodyLensSpec<OUT>(metas: List<Meta>,
                                 contentType: ContentType,
                                 get: LensGet<HttpMessage, OUT>,
                                 private val set: LensSet<HttpMessage, OUT>) : BodyLensSpec<OUT>(metas, contentType, get) {

    /**
     * Create another BiDiBodyLensSpec which applies the bi-directional transformations to the result. Any resultant Lens can be
     * used to extract or insert the final type from/into a Body.
     */
    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = BiDiBodyLensSpec(metas, contentType, get.map(nextIn), set.map(nextOut))

    /**
     * Create a lens for this Spec
     */
    override fun toLens(): BiDiBodyLens<OUT> {
        val getLens = get("")
        val setLens = set("")
        return BiDiBodyLens(metas, contentType,
            { getLens(it).let { if (it.isEmpty()) throw LensFailure(metas.map(::Missing), target = it) else it.first() } },
            { out: OUT, target: HttpMessage -> setLens(out?.let { listOf(it) } ?: emptyList(), target) }
        )
    }
}

fun httpBodyRoot(metas: List<Meta>, acceptedContentType: ContentType, contentNegotiation: ContentNegotiation) =
    BiDiBodyLensSpec<Body>(metas, acceptedContentType,
        LensGet { _, target ->
            contentNegotiation(acceptedContentType, CONTENT_TYPE(target))
            listOf(target.body)
        },
        LensSet { _, values, target -> values.fold(target) { memo, next -> memo.body(next) }.with(CONTENT_TYPE of acceptedContentType) }
    )

/**
 * Modes for determining if a passed content type is acceptable.
 */
fun interface ContentNegotiation {

    @Throws(LensFailure::class)
    operator fun invoke(expected: ContentType, actual: ContentType?)

    companion object {
        /**
         * The received Content-type header passed back MUST equal the expected Content-type, including directive
         */
        val Strict = ContentNegotiation { expected, actual -> if (actual != expected) throw LensFailure(Unsupported(CONTENT_TYPE.meta), target = actual) }

        /**
         * The received Content-type header passed back MUST equal the expected Content-type, not including the directive
         */
        val StrictNoDirective = ContentNegotiation { expected, actual -> if (expected.value != actual?.value) throw LensFailure(Unsupported(CONTENT_TYPE.meta), target = actual) }

        /**
         * If present, the received Content-type header passed back MUST equal the expected Content-type, including directive
         */
        val NonStrict = ContentNegotiation { expected, actual -> if (actual != null && actual != expected) throw LensFailure(Unsupported(CONTENT_TYPE.meta), target = actual) }

        /**
         * No validation is done on the received content type at all
         */
        val None = ContentNegotiation { _, _ -> }
    }
}

fun Body.Companion.string(contentType: ContentType, description: String? = null, contentNegotiation: ContentNegotiation = None) = httpBodyRoot(listOf(Meta(true, "body", StringParam, "body", description)), contentType, contentNegotiation)
    .map({ it.payload.asString() }, { Body(it) })

fun Body.Companion.nonEmptyString(contentType: ContentType, description: String? = null, contentNegotiation: ContentNegotiation = None) = string(contentType, description, contentNegotiation).map(StringBiDiMappings.nonEmpty())

fun Body.Companion.binary(contentType: ContentType, description: String? = null, contentNegotiation: ContentNegotiation = None) = httpBodyRoot(listOf(Meta(true, "body", FileParam, "body", description)), contentType, contentNegotiation)
    .map({ it.stream }, { Body(it) })

fun Body.Companion.regex(pattern: String, group: Int = 1, contentType: ContentType = ContentType.TEXT_PLAIN, description: String? = null, contentNegotiation: ContentNegotiation = None) =
    StringBiDiMappings.regex(pattern, group).let { string(contentType, description, contentNegotiation).map(it) }

internal fun <IN, NEXT> BiDiBodyLensSpec<IN>.map(mapping: BiDiMapping<IN, NEXT>) = map(mapping::invoke, mapping::invoke)
