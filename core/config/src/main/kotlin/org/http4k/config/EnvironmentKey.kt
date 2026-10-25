package org.http4k.config

import org.http4k.core.Uri
import org.http4k.lens.BiDiLens
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.BiDiMultiLensSpec
import org.http4k.lens.Lens
import org.http4k.lens.LensExtractor
import org.http4k.lens.LensFailure
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.Meta
import org.http4k.lens.Missing
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.EnumParam
import org.http4k.lens.StringBiDiMappings
import org.http4k.lens.int
import org.http4k.lens.mapWithNewMeta
import java.util.Locale.ROOT
import java.util.Locale.getDefault

open class EnvironmentKeySpec<OUT>(
    paramMeta: ParamMeta,
    private val envGet: LensGet<Environment, OUT>,
    private val envSet: LensSet<Environment, OUT>
) : BiDiLensSpec<Environment, OUT>("env", paramMeta, envGet, envSet) {

    override fun <NEXT> mapWithNewMeta(
        nextIn: (OUT) -> NEXT,
        nextOut: (NEXT) -> OUT,
        paramMeta: ParamMeta
    ): EnvironmentKeySpec<NEXT> =
        EnvironmentKeySpec(paramMeta, envGet.map(nextIn), envSet.map(nextOut))

    @Deprecated("Use the new argument in the multi() function instead")
    override val multi: BiDiMultiLensSpec<Environment, OUT> get() = throw UnsupportedOperationException("Use the new argument in the multi() function instead")
}

fun <OUT> BiDiLensSpec<Environment, OUT>.multi(separator: String): BiDiMultiLensSpec<Environment, OUT> =
    multi { _: Environment -> separator }

fun <OUT> BiDiLensSpec<Environment, OUT>.multi(separatorFn: (Environment) -> String): BiDiMultiLensSpec<Environment, OUT> {
    val spec = this

    return object : BiDiMultiLensSpec<Environment, OUT> {
        private fun getMulti(name: String, target: Environment): List<OUT> {
            val element = spec.optional(name)
            return target[name]?.split(separatorFn(target))?.map(String::trim).orEmpty()
                .mapNotNull { element(target.set(name, it)) }
        }

        private fun setMulti(name: String, values: List<OUT>, target: Environment): Environment {
            val element = spec.optional(name)
            return if (values.isEmpty()) {
                target - name
            } else {
                target.set(name, values.joinToString(separatorFn(target)) { element(it, Environment.EMPTY)[name]!! })
            }
        }

        private fun meta(required: Boolean, name: String, description: String?, metadata: Map<String, Any>) =
            Meta(required, spec.location, ArrayParam(spec.optional(name).meta.paramMeta), name, description, metadata)

        override fun defaulted(
            name: String,
            default: List<OUT>,
            description: String?,
            metadata: Map<String, Any>
        ): BiDiLens<Environment, List<OUT>> =
            defaulted(name, Lens(meta(false, name, description, metadata)) { default }, description, metadata)

        override fun defaulted(
            name: String,
            default: LensExtractor<Environment, List<OUT>>,
            description: String?,
            metadata: Map<String, Any>
        ): BiDiLens<Environment, List<OUT>> = BiDiLens(
            meta(false, name, description, metadata),
            { target -> getMulti(name, target).ifEmpty { default(target) } },
            { values, target -> setMulti(name, values, target) }
        )

        override fun optional(
            name: String,
            description: String?,
            metadata: Map<String, Any>
        ): BiDiLens<Environment, List<OUT>?> = BiDiLens(
            meta(false, name, description, metadata),
            { target -> getMulti(name, target).ifEmpty { null } },
            { values, target -> setMulti(name, values ?: emptyList(), target) }
        )

        override fun required(
            name: String,
            description: String?,
            metadata: Map<String, Any>
        ): BiDiLens<Environment, List<OUT>> {
            val meta = meta(true, name, description, metadata)
            return BiDiLens(
                meta,
                { target -> getMulti(name, target).ifEmpty { throw LensFailure(Missing(meta), target = target) } },
                { values, target -> setMulti(name, values, target) }
            )
        }
    }
}

/**
 * This models the key used to get a value out of the Environment using the standard Lens mechanic. Note that if your
 * values contain separators, use EnvironmentKey.(mapping).multi.required()/optional()/defaulted() or pass an explicit
 * separator into multi(separator) to retrieve the entire list.
 */
object EnvironmentKey : EnvironmentKeySpec<String>(
    ParamMeta.StringParam,
    LensGet { name, target -> listOfNotNull(target[name]) },
    LensSet { name, values, target ->
        values.fold(target - name) { acc, next ->
            acc.set(name, next)
        }
    }
) {
    object k8s {
        operator fun <T> invoke(fn: k8s.() -> T): T = fn(this)

        val SERVICE_PORT = int().required("SERVICE_PORT")
        val HEALTH_PORT = int().required("HEALTH_PORT")

        fun serviceUriFor(serviceName: String, isHttps: Boolean = false) = int()
            .map(serviceName.toUriFor(isHttps)) { it.port ?: 80 }
            .required("${serviceName.convertFromKey().uppercase(getDefault())}_SERVICE_PORT")

        private fun String.toUriFor(https: Boolean): (Int) -> Uri = {
            Uri.of("/")
                .scheme(if (https) "https" else "http")
                .authority(if (it == 80 || it == 443) this else "$this:$it")
        }
    }
}

inline fun <reified T : Enum<T>> EnvironmentKey.enum(caseSensitive: Boolean = true) = mapWithNewMeta(
    if (caseSensitive) StringBiDiMappings.enum<T>() else StringBiDiMappings.caseInsensitiveEnum(),
    EnumParam(T::class)
)

internal fun String.convertFromKey() = replace("_", "-").replace(".", "-").lowercase(ROOT)
