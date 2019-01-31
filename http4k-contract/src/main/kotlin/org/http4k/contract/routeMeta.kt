package org.http4k.contract

import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
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

sealed class HttpMessageMeta<out T : HttpMessage>(val message: T, val definitionId: String? = null)
class RequestMeta(request: Request, definitionId: String? = null) : HttpMessageMeta<Request>(request, definitionId)
class ResponseMeta(val description: String, response: Response, definitionId: String? = null) : HttpMessageMeta<Response>(response, definitionId)

class RouteMetaDsl internal constructor() {
    var summary: String = "<unknown>"
    var description: String? = null
    internal var request: RequestMeta? = null
    val tags = Appendable<Tag>()
    val produces = Appendable<ContentType>()
    val consumes = Appendable<ContentType>()
    internal val responses = Appendable<ResponseMeta>()
    var headers = Appendable<Lens<Request, *>>()
    var queries = Appendable<Lens<Request, *>>()
    internal var requestBody: BodyLens<*>? = null
    var operationId: String? = null

    /**
     * Add possible responses to this Route.
     */
    @JvmName("returningResponse")
    fun returning(vararg descriptionToResponse: Pair<String, Response>) =
        descriptionToResponse.forEach { (description, status) -> returning(ResponseMeta(description, status)) }

    /**
     * Add possible response metadata to this Route.
     */
    @JvmName("returningResponseMeta")
    fun returning(vararg responseMetas: ResponseMeta) {
        responseMetas.forEach { responses += it }
        responseMetas.forEach {
            produces += Header.CONTENT_TYPE(it.message)?.let { listOf(it) } ?: emptyList()
        }
    }

    /**
     * Add a possible response description/reason and status to this Route
     */
    @JvmName("returningStatus")
    fun returning(vararg statusesToDescriptions: Pair<Status, String>) =
        statusesToDescriptions.forEach { (status, d) -> returning(d to Response(status)) }

    /**
     * Add possible response statuses to this Route with no example.
     */
    @JvmName("returningStatus")
    fun returning(vararg statuses: Status) = statuses.forEach { returning(ResponseMeta("", Response(it))) }

    /**
     * Add an example response (using a Lens and a value) to this Route. It is also possible to pass in the definitionId for this response body which
     * will override the naturally generated one.
     */
    @JvmName("returningStatus")
    fun <T> returning(status: Status, body: Pair<BiDiBodyLens<T>, T>, description: String = "", definitionId: String? = null) {
        returning(ResponseMeta(description, Response(status).with(body.first of body.second), definitionId))
    }

    /**
     * Add an example request (using a Lens and a value) to this Route. It is also possible to pass in the definitionId for this request body which
     * will override the naturally generated one.
     */
    fun <T> receiving(body: Pair<BiDiBodyLens<T>, T>, definitionId: String? = null) {
        requestBody = body.first
        request = RequestMeta(Request(GET, "").with(body.first of body.second), definitionId)
    }

    /**
     * Set the input body type for this request WITHOUT an example. Hence the content-type will be registered but no
     * example schema will be generated.
     */
    fun <T> receiving(bodyLens: BiDiBodyLens<T>) {
        requestBody = bodyLens
    }
}

fun routeMetaDsl(fn: RouteMetaDsl.() -> Unit = {}) = RouteMetaDsl().apply(fn).run {
    RouteMeta(
        summary, description, request, tags.all.toSet(), requestBody, produces.all.toSet(), consumes.all.toSet(), queries.all + headers.all, responses.all, operationId
    )
}

data class Tag(val name: String, val description: String? = null)

data class RouteMeta(val summary: String = "<unknown>",
                     val description: String? = null,
                     val request: RequestMeta? = null,
                     val tags: Set<Tag> = emptySet(),
                     val body: BodyLens<*>? = null,
                     val produces: Set<ContentType> = emptySet(),
                     val consumes: Set<ContentType> = emptySet(),
                     val requestParams: List<Lens<Request, *>> = emptyList(),
                     val responses: List<ResponseMeta> = emptyList(),
                     val operationId: String? = null) {

    constructor(summary: String = "<unknown>", description: String? = null) : this(summary, description, null)
}
