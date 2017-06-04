package org.http4k.routing

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.BodyLens
import org.http4k.lens.Failure
import org.http4k.lens.Header
import org.http4k.lens.HeaderLens
import org.http4k.lens.Lens
import org.http4k.lens.LensFailure
import org.http4k.lens.QueryLens

data class Tag(val name: String, val description: String? = null)

data class Desc private constructor(val summary: String,
                                    val description: String?,
                                    val body: BodyLens<*>?,
                                    val request: Request? = null,
                                    val tags: Set<Tag> = emptySet(),
                                    val produces: Set<ContentType> = emptySet(),
                                    val consumes: Set<ContentType> = emptySet(),
                                    val requestParams: List<Lens<Request, *>> = emptyList(),
                                    val responses: Map<Status, Pair<String, Response>> = emptyMap()) : Filter {

    constructor(name: String = "<unknown>", description: String? = null) : this(name, description, null)

    fun header(new: HeaderLens<*>) = copy(requestParams = requestParams.plus(listOf(new)))
    fun query(new: QueryLens<*>) = copy(requestParams = requestParams.plus(listOf(new)))
    fun body(new: BodyLens<*>) = copy(body = new, consumes = consumes.plus(new.contentType))
    fun <T> body(new: Pair<BiDiBodyLens<T>, T>): Desc = copy(request = Request(GET, "").with(new.first of new.second)).body(new.first)

    fun taggedWith(tag: String) = taggedWith(Tag(tag))
    fun taggedWith(vararg new: Tag) = copy(tags = tags.plus(new))

    @JvmName("returningResponse")
    fun returning(new: Pair<String, Response>) =
        copy(
            produces = produces.plus(Header.Common.CONTENT_TYPE(new.second)?.let { listOf(it) } ?: emptyList()),
            responses = responses.plus(new.second.status to new))

    @JvmName("returningStatus")
    fun returning(new: Pair<String, Status>) = returning(new.first to Response(new.second))

    fun producing(vararg new: ContentType) = copy(produces = produces.plus(new))
    fun consuming(vararg new: ContentType) = copy(consumes = consumes.plus(new))

    override fun invoke(nextHandler: HttpHandler): HttpHandler =
        { req ->
            val body = body?.let { listOf(it::invoke) } ?: emptyList<(Request) -> Any?>()
            val errors = body.plus(requestParams).fold(emptyList<Failure>()) { memo, next ->
                try {
                    next(req)
                    memo
                } catch (e: LensFailure) {
                    memo.plus(e.failures)
                }
            }
            if (errors.isEmpty()) nextHandler(req) else throw LensFailure(errors)
        }
}