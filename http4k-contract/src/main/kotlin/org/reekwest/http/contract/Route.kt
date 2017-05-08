package org.http4k.http.contract

import org.http4k.http.contract.PathBinder.Companion.Core
import org.http4k.http.core.ContentType
import org.http4k.http.core.Filter
import org.http4k.http.core.Method
import org.http4k.http.core.Request
import org.http4k.http.core.Response
import org.http4k.http.core.Status
import org.http4k.http.lens.BodyLens
import org.http4k.http.lens.Failure
import org.http4k.http.lens.HeaderLens
import org.http4k.http.lens.Lens
import org.http4k.http.lens.LensFailure
import org.http4k.http.lens.QueryLens

class Route private constructor(internal val core: Core) {
    constructor(name: String, description: String? = null) : this(Core(name, description, null))

    fun header(new: HeaderLens<*>) = Route(core.copy(requestParams = core.requestParams.plus(listOf(new))))
    fun query(new: QueryLens<*>) = Route(core.copy(requestParams = core.requestParams.plus(listOf(new))))
    fun body(new: BodyLens<*>) = Route(core.copy(body = new))

    @JvmName("returningResponse")
    fun returning(new: Pair<String, Response>) = Route(core.copy(responses = core.responses.plus(new)))

    @JvmName("returningStatus")
    fun returning(new: Pair<String, Status>) = Route(core.copy(responses = core.responses.plus(new.first to Response(new.second))))

    fun producing(vararg new: ContentType) = Route(core.copy(produces = core.produces.plus(new)))
    fun consuming(vararg new: ContentType) = Route(core.copy(consumes = core.consumes.plus(new)))

    infix fun at(method: Method): PathBinder0 = PathBinder0(Core(this, method, { it }))

    companion object {
        internal data class Core(val summary: String,
                                 val description: String?,
                                 val body: BodyLens<*>?,
                                 val produces: Set<ContentType> = emptySet(),
                                 val consumes: Set<ContentType> = emptySet(),
                                 val requestParams: List<Lens<Request, *>> = emptyList(),
                                 val responses: List<Pair<String, Response>> = emptyList()) {

            internal val validationFilter = Filter {
                next ->
                {
                    val body = body?.let { listOf(it::invoke) } ?: emptyList<(Request) -> Any?>()
                    val errors = body.plus(requestParams).fold(emptyList<Failure>()) { memo, next ->
                        try {
                            next(it)
                            memo
                        } catch (e: LensFailure) {
                            memo.plus(e.failures)
                        }
                    }
                    if (errors.isEmpty()) next(it) else throw LensFailure(errors)
                }
            }
        }
    }
}
