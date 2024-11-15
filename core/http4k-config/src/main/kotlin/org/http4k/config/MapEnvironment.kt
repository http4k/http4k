package org.http4k.config

import org.http4k.lens.LensExtractor
import java.io.Reader
import java.util.Properties

class MapEnvironment private constructor(
    private val contents: Map<String, String>,
    override val separator: String = ","
) : Environment {
    override operator fun <T> get(key: LensExtractor<Environment, T>) = key(this)
    override operator fun get(key: String): String? = contents[key.convertFromKey()]
    override operator fun set(key: String, value: String) =
        MapEnvironment(contents + (key.convertFromKey() to value), separator)

    override fun minus(key: String): Environment = MapEnvironment(contents - key.convertFromKey(), separator)
    override fun keys() = contents.keys

    companion object {
        fun from(properties: Properties, separator: String = ","): Environment = MapEnvironment(
            properties.entries
                .fold(emptyMap()) { acc, (k, v) -> acc + (k.toString().convertFromKey() to v.toString()) }, separator
        )

        fun from(reader: Reader, separator: String = ","): Environment =
            from(Properties().apply { load(reader) }, separator)
    }
}
