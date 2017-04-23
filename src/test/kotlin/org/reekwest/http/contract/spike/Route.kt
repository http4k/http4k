package org.reekwest.http.contract.spike

import org.reekwest.http.contract.Lens
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.Request
import org.reekwest.http.core.Status

data class RouteResponse(val status: Status, val description: String?, val example: String?)

data class Route private constructor(val name: String,
                                     val description: String?,
                                     val produces: Set<ContentType>,
                                     val consumes: Set<ContentType>,
                                     val body: Lens<Request, *>?,
                                     val requestParams: Iterable<Lens<Request, *>>,
                                     val responses: Iterable<RouteResponse>) {

    constructor(name: String, description: String? = null) : this(name, description, emptySet(), emptySet(), null, emptyList(), emptyList())

    fun <T> taking(new: Lens<Request, *>) = copy(requestParams = requestParams.plus(new))
    fun <T> body(new: Lens<Request, *>) = copy(body = new)
    fun returning(new: Pair<Status, String>, description: String? = null) = copy(responses = responses.plus(RouteResponse(new.first, new.second, description)))
    fun producing(vararg new: ContentType) = copy(produces = produces.plus(new))
    fun consuming(vararg new: ContentType) = copy(consumes = consumes.plus(new))
}