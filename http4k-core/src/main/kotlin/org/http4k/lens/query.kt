package org.http4k.lens

import org.http4k.core.Request
import org.http4k.lens.ParamMeta.EnumParam
import org.http4k.lens.ParamMeta.StringParam

typealias QueryLens<T> = Lens<Request, T>

object Query : BiDiLensSpec<Request, String>("query", StringParam,
    LensGet { name, target -> target.queries(name).map { it ?: "" } },
    LensSet { name, values, target -> values.fold(target.removeQuery(name)) { m, next -> m.query(name, next) } }
)

inline fun <reified T : Enum<T>> Query.enum() = mapWithNewMeta(StringBiDiMappings.enum<T>(), EnumParam(T::class))
