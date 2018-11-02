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
    internal operator fun <T> get(key: Lens<Environment, T>) = key(this)
    internal operator fun get(key: String): String? = env[key] ?: env[key.convertFromKey()]

    internal operator fun set(key: String, value: String) = Environment(env + (key.convertFromKey() to value))

    infix fun overrides(that: Environment) = Environment(that.env + env)

    companion object {
        val EMPTY = from()

        /**
         * Configuration from the runtime environment
         */
        val ENV = Environment(System.getenv())

        /**
         * Configuration from JVM properties (-D flags)
         */
        val JVM_PROPERTIES = System.getProperties().toEnvironment()

        fun fromResource(resource: String) =
            Companion::class.java.getResourceAsStream("/${resource.removePrefix("/")}").reader().toProperties()

        fun from(file: File) = file.reader().toProperties()
        fun from(vararg pairs: Pair<String, String>) = Environment(pairs.toMap())
        fun defaults(vararg fn: (Environment) -> Environment) = fn.fold(EMPTY) { acc, next -> next(acc) }
        private fun Reader.toProperties() = Properties().apply { load(this@toProperties) }.toEnvironment()

        private fun Properties.toEnvironment() = Environment(entries
            .fold(emptyMap()) { acc, (k, v) -> acc.plus(k.toString().convertFromKey() to v.toString()) })
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
            .required("${serviceName.convertFromKey().toUpperCase()}_SERVICE_PORT")

        private fun String.toUriFor(https: Boolean): (Int) -> Uri = {
            Uri.of("/")
                .scheme(if (https) "https" else "http")
                .authority(if (it == 80 || it == 443) this else "$this:$it")
        }
    }
}

internal fun String.convertFromKey() = replace("_", "-").replace(".", "-").toLowerCase()