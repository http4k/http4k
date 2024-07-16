package org.http4k.config

import org.http4k.core.Uri
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.EnumParam
import org.http4k.lens.StringBiDiMappings
import org.http4k.lens.int
import org.http4k.lens.mapWithNewMeta
import java.util.Locale
import java.util.Locale.getDefault

/**
 * This models the key used to get a value out of the Environment using the standard Lens mechanic. Note that if your
 * values contain commas, either use a EnvironmentKey.(mapping).multi.required()/optional()/defaulted() to retrieve the
 * entire list, or override the comma separator in your initial Environment.
 */
object EnvironmentKey : BiDiLensSpec<Environment, String>("env", ParamMeta.StringParam,
    LensGet { name, target -> target[name]?.split(target.separator)?.map(String::trim).orEmpty() },
    LensSet { name, values, target ->
        values.fold(target - name) { acc, next ->
            val existing = acc[name]?.let { listOf(it) }.orEmpty()
            acc.set(name, (existing + next).joinToString(target.separator))
        }
    }
) {
    object k8s {
        operator fun <T> invoke(fn: k8s.() -> T): T = fn(this)

        val SERVICE_PORT = int().required("SERVICE_PORT")
        val HEALTH_PORT = int().required("HEALTH_PORT")

        fun serviceUriFor(serviceName: String, isHttps: Boolean = false) = int()
            .map(serviceName.toUriFor(isHttps)) { it.port ?: 80 }
            .required("${serviceName.convertFromKey().uppercase(Locale.getDefault())}_SERVICE_PORT")

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

internal fun String.convertFromKey() = replace("_", "-").replace(".", "-").lowercase(getDefault())
