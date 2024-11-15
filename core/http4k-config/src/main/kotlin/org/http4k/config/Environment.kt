package org.http4k.config

import org.http4k.lens.LensExtractor
import java.io.File
import java.io.FileNotFoundException

/**
 * This models the runtime environment of the shell where the app is running. Optionally pass a separator to use for
 * multi-values otherwise a standard comma is used - this means you MUST override the separator if you have single values
 * which contain commas, otherwise singular environment keys will just retrieve the first value.
 */
interface Environment {
    val separator: String get() = ","

    fun keys(): Set<String>

    operator fun <T> get(key: LensExtractor<Environment, T>): T

    operator fun get(key: String): String?

    operator fun minus(key: String): Environment

    operator fun set(key: String, value: String): Environment

    /**
     * Overlays the configuration set in this Environment on top of the values in the passed Environment.
     * Used to chain: eg. Local File -> System Properties -> Env Properties -> Defaults
     */
    infix fun overrides(that: Environment): Environment = MapEnvironment.from(
        (that.keys().map { it to that[it]!! } + keys().map { it to this[it]!! }).toMap().toProperties(),
        separator = separator
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
        fun fromResource(resource: String) =
            Companion::class.java.getResourceAsStream("/${resource.removePrefix("/")}")
                ?.let { MapEnvironment.from(it.reader()) } ?: throw FileNotFoundException(resource)

        /**
         * Load configuration from standard Properties file format on disk
         */
        fun from(file: File): Environment = MapEnvironment.from(file.reader())

        /**
         * Setup default configuration mappings using EnvironmentKey lens bindings
         */
        fun defaults(vararg fn: (Environment) -> Environment) = fn.fold(EMPTY) { acc, next -> next(acc) }

        fun from(vararg pairs: Pair<String, String>, separator: String = ","): Environment =
            MapEnvironment.from(pairs.toMap().toProperties(), separator)

        fun from(env: Map<String, String>, separator: String = ","): Environment = MapEnvironment.from(env.toProperties(), separator)
    }
}

