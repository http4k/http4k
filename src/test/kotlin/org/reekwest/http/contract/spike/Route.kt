package org.reekwest.http.contract.spike

import org.reekwest.http.contract.Lens
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Request
import org.reekwest.http.core.Status

data class RouteResponse(val status: Status, val description: String?, val example: String?)

data class Route private constructor(private val name: String,
                                     private val description: String?,
                                     private val body: Lens<Request, *>?,
                                     private val produces: Set<ContentType> = emptySet(),
                                     private val consumes: Set<ContentType> = emptySet(),
                                     private val requestParams: Iterable<Lens<Request, *>> = emptyList(),
                                     private val responses: Iterable<RouteResponse> = emptyList()) {

    constructor(name: String, description: String? = null) : this(name, description, null)

    fun taking(new: Lens<Request, *>) = copy(requestParams = requestParams.plus(new))
    fun body(new: Lens<HttpMessage, *>) = copy(body = new)
    fun returning(new: Pair<Status, String>, description: String? = null) = copy(responses = responses.plus(RouteResponse(new.first, new.second, description)))
    fun producing(vararg new: ContentType) = copy(produces = produces.plus(new))
    fun consuming(vararg new: ContentType) = copy(consumes = consumes.plus(new))
}