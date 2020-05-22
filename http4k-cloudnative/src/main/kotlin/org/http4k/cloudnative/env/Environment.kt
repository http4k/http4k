package org.http4k.cloudnative.env

import org.http4k.core.Uri
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.Lens
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.ParamMeta
import org.http4k.lens.int
import java.io.File
import java.io.Reader
import java.util.Properties


/**
 * This models the runtime environment of the shell where the app is running. Optionally pass a separator to use for
 * multi-values otherwise a standard comma is used - this means you MUST override the separator if you have single values
 * which contain commas, otherwise singular environment keys will just retrieve the first value.
 */
interface Environment {
    val separator: String get() = ","

    fun keys(): Set<String>

    operator fun <T> get(key: Lens<Environment, T>): T

    operator fun get(key: String): String?

    operator fun minus(key: String): Environment

    operator fun set(key: String, value: String): Environment

    /**
     * Overlays the configuration set in this Environment on top of the values in the passed Environment.
     * Used to chain: eg. Local File -> System Properties -> Env Properties -> Defaults
     */
    infix fun overrides(that: Environment): Environment = MapEnvironment.from(
        (that.keys().map { it to that[it]!! } + keys().map { it to this[it]!! }).toMap().toProperties(), separator = separator
    )

    companion object {
        val EMPTY: Environment = from()

        /**
         * Configuration from the runtime environment
         */
        val ENV: Environment = MapEnvironment.from(System.getenv().toProperties())

        /**
         * Configuration from JVM properties (-D flags)
         */
        val JVM_PROPERTIES: Environment = MapEnvironment.from(System.getProperties())

        /**
         * Load configuration from standard Properties file format on classpath
         */
        fun fromResource(resource: String): Environment =
            MapEnvironment.from(Companion::class.java.getResourceAsStream("/${resource.removePrefix("/")}").reader())

        /**
         * Load configuration from standard Properties file format on disk
         */
        fun from(file: File): Environment = MapEnvironment.from(file.reader())

        /**
         * Setup default configuration mappings using EnvironmentKey lens bindings
         */
        fun defaults(vararg fn: (Environment) -> Environment) = fn.fold(EMPTY) { acc, next -> next(acc) }

        fun from(vararg pairs: Pair<String, String>): Environment = MapEnvironment.from(pairs.toMap().toProperties())
    }
}

class MapEnvironment private constructor(private val contents: Map<String, String>, override val separator: String = ",") : Environment {
    override operator fun <T> get(key: Lens<Environment, T>) = key(this)
    override operator fun get(key: String): String? = contents[key.convertFromKey()]
    override operator fun set(key: String, value: String) = MapEnvironment(contents + (key.convertFromKey() to value), separator)
    override fun minus(key: String): Environment = MapEnvironment(contents - key.convertFromKey(), separator)
    override fun keys() = contents.keys

    companion object {
        fun from(properties: Properties, separator: String = ","): Environment = MapEnvironment(properties.entries
            .fold(emptyMap()) { acc, (k, v) -> acc + (k.toString().convertFromKey() to v.toString()) }, separator)

        fun from(reader: Reader, separator: String = ","): Environment = from(Properties().apply { load(reader) }, separator)
    }
}

/**
 * This models the key used to get a value out of the Environment using the standard Lens mechanic. Note that if your
 * values contain commas, either use a EnvironmentKey.(mapping).multi.required()/optional()/defaulted() to retrieve the
 * entire list, or override the comma separator in your initial Environment.
 */
object EnvironmentKey : BiDiLensSpec<Environment, String>("env", ParamMeta.StringParam,
    LensGet { name, target -> target[name]?.split(target.separator)?.map(String::trim) ?: emptyList() },
    LensSet { name, values, target ->
        values.fold(target - name) { acc, next ->
            val existing = acc[name]?.let { listOf(it) } ?: emptyList()
            acc.set(name, (existing + next).joinToString(target.separator))
        }
    }
) {
    object k8s {
        operator fun <T> invoke(fn: k8s.() -> T): T = fn(this)

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
