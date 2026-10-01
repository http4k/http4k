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
    get: LensGet<Environment, OUT>,
    set: LensSet<Environment, OUT>
) : BiDiLensSpec<Environment, OUT>("env", paramMeta, get, set) {

    override fun <NEXT> mapWithNewMeta(
        nextIn: (OUT) -> NEXT,
        nextOut: (NEXT) -> OUT,
        paramMeta: ParamMeta
    ): EnvironmentKeySpec<NEXT> =
        EnvironmentKeySpec(paramMeta, get.map(nextIn), set.map(nextOut))

    override val multi: BiDiMultiLensSpec<Environment, OUT> get() = multi()

    fun multi(separator: String = ","): BiDiMultiLensSpec<Environment, OUT> = multi { separator }

    fun multi(separatorFn: (Environment) -> String): BiDiMultiLensSpec<Environment, OUT> = object : BiDiMultiLensSpec<Environment, OUT> {
        private fun getMulti(name: String, target: Environment): List<OUT> {
            val sep = separatorFn(target)
            return target[name]?.split(sep)?.map(String::trim).orEmpty().flatMap { s ->
                get(name)(target.set(name, s))
            }
        }

        private fun setMulti(name: String, values: List<OUT>, target: Environment): Environment {
            val sep = separatorFn(target)
            return if (values.isEmpty()) {
                target - name
            } else {
                val stringValues = values.map { v ->
                    set(name)(listOf(v), Environment.EMPTY)[name]!!
                }
                target.set(name, stringValues.joinToString(sep))
            }
        }

        override fun defaulted(
            name: String,
            default: List<OUT>,
            description: String?,
            metadata: Map<String, Any>
        ): BiDiLens<Environment, List<OUT>> =
            defaulted(
                name,
                Lens(Meta(false, location, ArrayParam(paramMeta), name, description, metadata)) { default },
                description,
                metadata
            )

        override fun defaulted(
            name: String,
            default: LensExtractor<Environment, List<OUT>>,
            description: String?,
            metadata: Map<String, Any>
        ): BiDiLens<Environment, List<OUT>> {
            val meta = Meta(false, location, ArrayParam(paramMeta), name, description, metadata)
            return BiDiLens(
                meta,
                { target ->
                    if (target[name] == null) {
                        default(target)
                    } else {
                        getMulti(name, target).ifEmpty { default(target) }
                    }
                },
                { values, target -> setMulti(name, values, target) }
            )
        }

        override fun optional(
            name: String,
            description: String?,
            metadata: Map<String, Any>
        ): BiDiLens<Environment, List<OUT>?> {
            val meta = Meta(false, location, ArrayParam(paramMeta), name, description, metadata)
            return BiDiLens(
                meta,
                { target ->
                    if (target[name] == null) {
                        null
                    } else {
                        getMulti(name, target).ifEmpty { null }
                    }
                },
                { values, target -> setMulti(name, values ?: emptyList(), target) }
            )
        }

        override fun required(
            name: String,
            description: String?,
            metadata: Map<String, Any>
        ): BiDiLens<Environment, List<OUT>> {
            val meta = Meta(true, location, ArrayParam(paramMeta), name, description, metadata)
            return BiDiLens(
                meta,
                { target ->
                    if (target[name] == null) throw LensFailure(Missing(meta), target = target)
                    getMulti(name, target).ifEmpty {
                        throw LensFailure(Missing(meta), target = target)
                    }
                },
                { values, target -> setMulti(name, values, target) }
            )
        }
    }
}

fun <OUT> BiDiLensSpec<Environment, OUT>.multi(separator: String): BiDiMultiLensSpec<Environment, OUT> =
    (this as? EnvironmentKeySpec<OUT>)?.multi(separator) ?: multi { separator }

fun <OUT> BiDiLensSpec<Environment, OUT>.multi(separatorFn: (Environment) -> String): BiDiMultiLensSpec<Environment, OUT> =
    (this as? EnvironmentKeySpec<OUT>)?.multi(separatorFn) ?: EnvironmentKeySpec(paramMeta, get, set).multi(separatorFn)

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
