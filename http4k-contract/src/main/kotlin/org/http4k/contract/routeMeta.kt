package org.http4k.contract

import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.BodyLens
import org.http4k.lens.Header
import org.http4k.lens.Lens

data class Tag(val name: String, val description: String? = null)

data class RouteMeta(val summary: String = "<unknown>",
                     val description: String? = null,
                     val request: Request? = null,
                     val tags: Set<Tag> = emptySet(),
                     val body: BodyLens<*>? = null,
                     val produces: Set<ContentType> = emptySet(),
                     val consumes: Set<ContentType> = emptySet(),
                     val requestParams: List<Lens<Request, *>> = emptyList(),
                     val responses: Map<Status, Pair<String, Response>> = emptyMap()) {

    constructor(summary: String = "<unknown>", description: String? = null) : this(summary, description, null)

    fun taggedWith(tag: String) = taggedWith(Tag(tag))
    fun taggedWith(vararg new: Tag) = copy(tags = tags.plus(new))

    operator fun plus(new: Lens<Request, *>): RouteMeta = copy(requestParams = requestParams.plus(listOf(new)))
    operator fun plus(new: BodyLens<*>): RouteMeta = copy(body = new)

    @JvmName("returningResponse")
    fun returning(new: Pair<String, Response>) =
        copy(
            produces = produces.plus(Header.Common.CONTENT_TYPE(new.second)?.let { listOf(it) } ?: emptyList()),
            responses = responses.plus(new.second.status to new))

    @JvmName("returningStatus")
    fun returning(new: Pair<String, Status>) = returning(new.first to Response(new.second))

    fun <T> receiving(new: Pair<BiDiBodyLens<T>, T>): RouteMeta = copy(request = Request(GET, "").with(new.first of new.second))

    fun producing(vararg new: ContentType) = copy(produces = produces.plus(new))
    fun consuming(vararg new: ContentType) = copy(consumes = consumes.plus(new))


}

class MetaDsl {
    var summary: String = "<unknown>"
    var description: String? = null
    var request: Request? = null
    var body: BodyLens<*>? = null
    val tags: MutableSet<Tag> = mutableSetOf()
    val produces: MutableSet<ContentType> = mutableSetOf()
    val consumes: MutableSet<ContentType> = mutableSetOf()
    val queries: List<Lens<Request, *>> = mutableListOf()
    val headers: List<Lens<Request, *>> = mutableListOf()
    internal val responses: MutableMap<Status, Pair<String, Response>> = mutableMapOf()

    @JvmName("returningResponse")
    fun returning(new: Pair<String, Response>) {
        produces.plus(Header.Common.CONTENT_TYPE(new.second)?.let { listOf(it) } ?: emptyList())
        responses += responses.plus(new.second.status to new)
    }

    @JvmName("returningStatus")
    fun returning(new: Pair<String, Status>) = returning(new.first to Response(new.second))

    @JvmName("returningStatus")
    fun returning(new: Status) = returning("" to Response(new))

    fun <T> receiving(new: Pair<BiDiBodyLens<T>, T>) {
        request = Request(GET, "").with(new.first of new.second)
    }
}

fun meta(fn: MetaDsl.() -> Unit): RouteMeta = MetaDsl().apply(fn).run {
    RouteMeta(summary, description, request, tags, body, produces, consumes, queries.plus(headers), responses)
}