package org.http4k.contract

import org.http4k.core.ContentType
import org.http4k.core.Method
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
    var operationId: String? = null

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
        summary, description, request, tags.all.toSet(), body, produces.all.toSet(), consumes.all.toSet(), queries.all + headers.all, responses.all.toMap(), operationId
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
                     val responses: Map<Status, Pair<String, Response>> = emptyMap(),
                     val operationId: String? = null) {

    constructor(summary: String = "<unknown>", description: String? = null) : this(summary, description, null)

    operator fun plus(new: Lens<Request, *>): RouteMeta = copy(requestParams = requestParams.plus(listOf(new)))
    operator fun plus(new: BodyLens<*>): RouteMeta = copy(body = new)
}
