package org.http4k.cloudnative.env

import org.http4k.core.Uri
import org.http4k.lens.*
import java.io.File
import java.io.Reader
import java.util.*

/**
 * This models the runtime environment of the shell where the app is running
 */
data class Environment private constructor(private val env: Map<String, String>) {
    internal operator fun get(key: String) = env[key] ?: env[key.convertToKey()]
    internal operator fun set(key: String, value: String): Environment = Environment(env + (key.convertToKey() to value))

    infix fun overrides(that: Environment): Environment = Environment(that.env + env)

    companion object {
        val ENV = Environment(System.getenv())
        val JVM_PROPERTIES = System.getProperties().toEnvironment()
        val SYSTEM = JVM_PROPERTIES overrides ENV

        private fun Properties.toEnvironment(): Environment {
            return Environment(entries
                .fold(emptyMap()) { acc, (k, v) ->
                    acc.plus(k.toString() to v.toString())
                })
        }

        val EMPTY = from()

        fun fromFile(file: File) = file.reader().toProperties()
        fun fromResource(resource: String) = Environment.Companion::class.java.getResourceAsStream("/${resource.removePrefix("/")}").reader().toProperties()
        fun from(vararg pairs: Pair<String, String>) = Environment(pairs.toMap())
        fun defaults(vararg fn: (Environment) -> Environment) = fn.fold(EMPTY) { acc, next -> next(acc) }
        private fun Reader.toProperties() = Properties().apply { load(this@toProperties) }.toEnvironment()
    }
}

object EnvironmentKey : BiDiLensSpec<Environment, String>("env", ParamMeta.StringParam,
    LensGet { name, target -> target[name]?.let { listOf(it) } ?: emptyList() },
    LensSet { name, values, target -> values.fold(target) { acc, next -> acc.set(name, next) } }
) {
    object k8s {
        operator fun <T> invoke(fn: EnvironmentKey.k8s.() -> T): T = fn(this)

        val SERVICE_PORT = int().required("SERVICE_PORT")
        val HEALTH_PORT = int().required("HEALTH_PORT")

        fun serviceUriFor(serviceName: String, isHttps: Boolean = false) = int()
            .map(serviceName.toUriFor(isHttps), { it.port ?: 80 })
            .required("${serviceName.convertToKey()}_SERVICE_PORT")

        private fun String.toUriFor(https: Boolean): (Int) -> Uri = {
            Uri.of("/")
                .scheme(if (https) "https" else "http")
                .authority(if (it == 80 || it == 443) this else "$this:$it")
        }
    }
}

internal fun String.convertToKey() =
    flatMap { if (it.isLetter() && it.isUpperCase()) listOf('_', it) else listOf(it) }
        .joinToString("")
        .replace("-", "_")
        .replace(".", "_")
        .toUpperCase()