package org.http4k.contract

import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.BodyLens
import org.http4k.lens.Header
import org.http4k.lens.Lens
import org.http4k.util.Appendable

class RouteMetaDsl internal constructor() {
    var summary: String = "<unknown>"
    var description: String? = null
    var request: Request? = null
    val tags = Appendable<Tag>()
    val produces = Appendable<ContentType>()
    val consumes = Appendable<ContentType>()
    internal val responses = Appendable<Pair<Status, Pair<String, Response>>>()
    var headers = Appendable<Lens<Request, *>>()
    var queries = Appendable<Lens<Request, *>>()
    var body: BodyLens<*>? = null

    @JvmName("returningResponse")
    fun returning(new: Pair<String, Response>) {
        produces += (Header.Common.CONTENT_TYPE(new.second)?.let { listOf(it) } ?: emptyList())
        responses += new.second.status to new
    }

    @JvmName("returningStatus")
    fun returning(new: Pair<String, Status>) = returning(new.first to Response(new.second))

    @JvmName("returningStatus")
    fun returning(new: Status) = returning("" to Response(new))

    fun <T> receiving(new: Pair<BiDiBodyLens<T>, T>) {
        request = Request(Method.GET, "").with(new.first of new.second)
    }
}

fun routeMetaDsl(fn: RouteMetaDsl.() -> Unit = {}) = RouteMetaDsl().apply(fn).run {
    RouteMeta(
        summary, description, request, tags.all.toSet(), body, produces.all.toSet(), consumes.all.toSet(), queries.all + headers.all, responses.all.toMap()
    )
}
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

    operator fun plus(new: Lens<Request, *>): RouteMeta = copy(requestParams = requestParams.plus(listOf(new)))
    operator fun plus(new: BodyLens<*>): RouteMeta = copy(body = new)

    @JvmName("returningResponse")
    @Deprecated("use meta builder instead")
    fun RouteMeta.returning(new: Pair<String, Response>) =
        copy(
            produces = produces.plus(Header.Common.CONTENT_TYPE(new.second)?.let { listOf(it) } ?: emptyList()),
            responses = responses.plus(new.second.status to new))

    @JvmName("returningStatus")
    @Deprecated("use meta builder instead")
    fun RouteMeta.returning(new: Pair<String, Status>) = returning(new.first to Response(new.second))

    @Deprecated("use meta builder instead")
    fun <T> RouteMeta.receiving(new: Pair<BiDiBodyLens<T>, T>): RouteMeta = copy(request = Request(GET, "").with(new.first of new.second))

    @Deprecated("use meta builder instead")
    fun RouteMeta.producing(vararg new: ContentType) = copy(produces = produces.plus(new))
    @Deprecated("use meta builder instead")
    fun RouteMeta.consuming(vararg new: ContentType) = copy(consumes = consumes.plus(new))
    @Deprecated("use meta builder instead")
    fun RouteMeta.taggedWith(tag: String) = taggedWith(Tag(tag))
    @Deprecated("use meta builder instead")
    fun RouteMeta.taggedWith(vararg new: Tag) = copy(tags = tags.plus(new))
}