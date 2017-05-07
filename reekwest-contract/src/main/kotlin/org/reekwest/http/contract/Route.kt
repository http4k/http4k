package org.reekwest.http.contract

import org.reekwest.http.contract.PathBinder.Companion.Core
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.Filter
import org.reekwest.http.core.Method
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import org.reekwest.http.lens.BiDiBodyLens
import org.reekwest.http.lens.BodyLens
import org.reekwest.http.lens.Failure
import org.reekwest.http.lens.HeaderLens
import org.reekwest.http.lens.Lens
import org.reekwest.http.lens.LensFailure
import org.reekwest.http.lens.QueryLens

class Route private constructor(internal val core: Core) {
    constructor(name: String, description: String? = null) : this(Core(name, description, null))

    fun header(new: HeaderLens<*>) = Route(core.copy(requestParams = core.requestParams.plus(listOf(new))))
    fun query(new: QueryLens<*>) = Route(core.copy(requestParams = core.requestParams.plus(listOf(new))))
    fun body(new: BiDiBodyLens<*>) = Route(core.copy(body = new))

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
