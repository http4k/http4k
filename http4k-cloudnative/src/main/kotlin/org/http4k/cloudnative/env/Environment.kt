package org.http4k.cloudnative.env

import org.http4k.core.Uri
import org.http4k.lens.*

/**
 * This models the runtime environment of the shell where the app is running
 */
data class Environment private constructor(private val env: Map<String, String>) {
    internal operator fun get(key: String) = env[key] ?: env[key.convertToKey()]
    internal operator fun set(key: String, value: String): Environment = Environment(env + (key.convertToKey() to value))

    infix fun overrides(that: Environment): Environment = Environment(that.env + env)

    companion object {
        /**
         * Use this inside K8s
         */
        val ENV = Environment(System.getenv())
        val JVM_PROPERTIES = Environment(System.getProperties().entries
            .fold(emptyMap()) { acc, (k, v) ->
                acc.plus(k.toString() to v.toString())
            })
        val EMPTY = from()

        fun from(vararg pairs: Pair<String, String>) = Environment(pairs.toMap())
    }
}

object EnvironmentKey : BiDiLensSpec<Environment, String>("env", ParamMeta.StringParam,
    LensGet { name, target -> target[name]?.let { listOf(it) } ?: emptyList() },
    LensSet { name, values, target -> values.fold(target) { acc, next -> acc.set(name, next) } }
) {
    object k8s {
        val HEALTH_PORT = int().required("SERVICE_PORT")

        val SERVICE_PORT = int().required("HEALTH_PORT")

        fun baseServiceUriFor(serviceName: String, https: Boolean = false) = int()
            .map {
                Uri.of("/")
                    .scheme(if (https) "https" else "http")
                    .authority(if (it == 80 || it == 443) serviceName else "$serviceName:$it")
            }
            .required("${serviceName.convertToKey()}_SERVICE_PORT")
    }
}

internal fun String.convertToKey() =
    flatMap { if (it.isLetter() && it.isUpperCase()) listOf('_', it) else listOf(it) }
        .joinToString("")
        .replace("-", "_")
        .replace(".", "_")
        .toUpperCase()