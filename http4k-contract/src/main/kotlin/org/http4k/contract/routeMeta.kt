package org.http4k.contract

import org.http4k.contract.security.Security
import org.http4k.core.*
import org.http4k.core.Method.POST
import org.http4k.format.AutoContentNegotiator
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.BodyLens
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.Lens
import org.http4k.util.Appendable

open class HttpMessageMeta<out T : HttpMessage>(
    val message: T,
    val description: String,
    val definitionId: String?,
    val example: Any?,
    val schemaPrefix: String? = null
)

class RequestMeta(request: Request, definitionId: String? = null, example: Any? = null, schemaPrefix: String? = null) :
    HttpMessageMeta<Request>(request, "request", definitionId, example, schemaPrefix)

class ResponseMeta(
    description: String,
    response: Response,
    definitionId: String? = null,
    example: Any? = null,
    schemaPrefix: String? = null
) : HttpMessageMeta<Response>(response, description, definitionId, example, schemaPrefix)

class RouteMetaDsl internal constructor() {
    var summary: String = "<unknown>"
    var description: String? = null
    val tags = Appendable<Tag>()
    val produces = Appendable<ContentType>()
    val consumes = Appendable<ContentType>()
    internal val requests = Appendable<HttpMessageMeta<Request>>()
    internal val responses = Appendable<HttpMessageMeta<Response>>()
    val headers = Appendable<Lens<Request, *>>()
    val queries = Appendable<Lens<Request, *>>()
    val cookies = Appendable<Lens<Request, *>>()
    internal var requestBody: BodyLens<*>? = null
    var operationId: String? = null
    var security: Security? = null
    var preFlightExtraction: PreFlightExtraction? = null
    internal var deprecated: Boolean = false
    internal val callbacks = mutableMapOf<String, MutableMap<Uri, WebCallback>>()

    /**
     * Add possible responses to this Route.
     */
    @JvmName("returningResponse")
    fun returning(vararg descriptionToResponse: Pair<String, Response>) =
        descriptionToResponse.forEach { (description, status) -> returning(ResponseMeta(description, status)) }

    /**
     * Add possible response metadata to this Route. A route supports multiple possible responses.
     */
    @JvmName("returningResponseMeta")
    fun returning(vararg responseMetas: HttpMessageMeta<Response>) {
        responseMetas.forEach { responses += it }
        responseMetas.forEach {
            produces += CONTENT_TYPE(it.message)?.let { listOf(it) } ?: emptyList()
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
     * Add an example response (using a Lens and a value) to this Route. It is also possible to pass in the definitionId
     * for this response body which will override the naturally generated one.
     */
    @JvmName("returningStatus")
    fun <T> returning(
        status: Status, body: Pair<BiDiBodyLens<T>, T>,
        description: String? = null,
        definitionId: String? = null,
        schemaPrefix: String? = null
    ) {
        returning(
            ResponseMeta(
                description
                    ?: status.description,
                Response(status).with(body.first of body.second),
                definitionId,
                body.second,
                schemaPrefix
            )
        )
    }

    /**
     * Add all negotiated responses, with samples.  It is also possible to pass in the definitionId
     * for this response body which will override the naturally generated one.
     */
    @JvmName("returningStatusNegotiated")
    fun <T : Any> returning(
        status: Status,
        negotiated: Pair<AutoContentNegotiator<T>, T>,
        description: String? = null,
        definitionId: String? = null,
        schemaPrefix: String? = null
    ) {
        for (lens in negotiated.first) {
            returning(status, lens to negotiated.second, description, definitionId, schemaPrefix)
        }
    }

    /**
     * Add an example request (using a Lens and a value) to this Route. It is also possible to pass in the definitionId
     * for this request body which will override the naturally generated one.
     */
    fun <T> receiving(
        body: Pair<BiDiBodyLens<T>, T>,
        definitionId: String? = null,
        schemaPrefix: String? = null
    ) {
        requestBody = body.first
        receiving(
            RequestMeta(
                Request(POST, "").with(body.first of body.second),
                definitionId,
                body.second,
                schemaPrefix
            )
        )
    }

    /**
     * Add negotiated example requests to this route.  It is also possible to pass in the definitionId
     * for this request body which will override the naturally generated one.
     */
    @JvmName("receivingNegotiated")
    fun <T> receiving(
        negotiated: Pair<AutoContentNegotiator<T>, T>,
        definitionId: String? = null,
        schemaPrefix: String? = null
    ) {
        for (lens in negotiated.first) {
            receiving(lens to negotiated.second, definitionId, schemaPrefix)
        }
        requestBody = negotiated.first.toBodyLens()
    }

    /**
     * Add request metadata to this Route. A route only supports a single possible request.
     */
    fun receiving(requestMeta: HttpMessageMeta<Request>) {
        requests += requestMeta
        consumes += CONTENT_TYPE(requestMeta.message)?.let { listOf(it) } ?: emptyList()
    }

    /**
     * Set the input body type for this request WITHOUT an example. Hence the content-type will be registered but no
     * example schema will be generated.
     */
    fun <T> receiving(bodyLens: BiDiBodyLens<T>) {
        requestBody = bodyLens
        receiving(RequestMeta(Request(POST, "").with(CONTENT_TYPE of bodyLens.contentType)))
    }

    fun markAsDeprecated() {
        deprecated = true
    }

    fun callback(name: String, fn: () -> Pair<String, WebCallback>) {
        callbacks.getOrPut(name) { mutableMapOf() } += fn().let { Uri.of(it.first) to it.second }
    }
}

infix fun ContractRouteSpec0.bindCallback(method: Method): Pair<String, WebCallback> =
    pathFn(Root).toString() to WebCallback(method, routeMeta)

infix fun ContractRouteSpec0.bindWebhook(method: Method) = WebCallback(method, routeMeta)

fun routeMetaDsl(fn: RouteMetaDsl.() -> Unit = {}) = RouteMetaDsl().apply(fn).run {
    RouteMeta(
        summary,
        description,
        tags.all.toSet(),
        requestBody,
        produces.all.toSet(),
        consumes.all.toSet(),
        queries.all + headers.all + cookies.all,
        requests.all,
        responses.all,
        preFlightExtraction,
        security,
        operationId,
        deprecated,
        callbacks.takeIf { it.isNotEmpty() }
    )
}

data class RouteMeta(
    val summary: String = "<unknown>",
    val description: String? = null,
    val tags: Set<Tag> = emptySet(),
    val body: BodyLens<*>? = null,
    val produces: Set<ContentType> = emptySet(),
    val consumes: Set<ContentType> = emptySet(),
    val requestParams: List<Lens<Request, *>> = emptyList(),
    val requests: List<HttpMessageMeta<Request>> = emptyList(),
    val responses: List<HttpMessageMeta<Response>> = emptyList(),
    val preFlightExtraction: PreFlightExtraction? = null,
    val security: Security? = null,
    val operationId: String? = null,
    val deprecated: Boolean = false,
    val callbacks: Map<String, Map<Uri, WebCallback>>? = null
)

data class WebCallback(val method: Method, val meta: RouteMeta)
