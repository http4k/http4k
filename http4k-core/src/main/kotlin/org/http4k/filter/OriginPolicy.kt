package org.http4k.filter

/**
 * For creating custom origin policy for allowing CORS
 */
interface OriginPolicy : (String) -> Boolean {
    companion object
}

/**
 * Allows all origins for CORS
 */
fun OriginPolicy.Companion.AllowAll() = AllowAllOriginPolicy

object AllowAllOriginPolicy : OriginPolicy {
    override fun invoke(origin: String) = true
}

/**
 * Allows a given single origin for CORS
 */
fun OriginPolicy.Companion.Only(allowedOrigin: String) = object : OriginPolicy {
    override fun invoke(origin: String) = allowedOrigin == origin
}

/**
 * Allows a given list of origins for CORS
 */
fun OriginPolicy.Companion.AnyOf(allowedOrigins: List<String>) = object : OriginPolicy {
    override fun invoke(origin: String) = origin in allowedOrigins
}

/**
 * Allows a given list of origins for CORS
 */
fun OriginPolicy.Companion.AnyOf(vararg allowedOrigins: String) = AnyOf(allowedOrigins.toList())

/**
 * Allows origin(s) matching a Regex for CORS
 */
fun OriginPolicy.Companion.Pattern(originRegex: Regex) = object : OriginPolicy {
    override fun invoke(origin: String) = originRegex.matches(origin)
}
