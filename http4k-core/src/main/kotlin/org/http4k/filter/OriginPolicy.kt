package org.http4k.filter

/**
 * For creating custom origin policy for allowing CORS
 */
interface OriginPolicy: (String) -> Boolean

/**
 * Allows all origins for CORS
 */
class AllowAllOriginPolicy : OriginPolicy {
    override fun invoke(origin: String): Boolean {
        return true
    }
}

/**
 * Allows a given single origin for CORS
 */
data class SingleOriginPolicy(val allowedOrigin: String) : OriginPolicy {
    override fun invoke(origin: String): Boolean {
        return allowedOrigin == origin
    }
}

/**
 * Allows a given list of origins for CORS
 */
data class MultipleOriginPolicy(val allowedOrigins: List<String>) : OriginPolicy {
    override fun invoke(origin: String): Boolean {
        return origin in allowedOrigins
    }
}

/**
 * Allows origin(s) matching a Regex for CORS
 */
data class PatternOriginPolicy(val originRegex: Regex) : OriginPolicy {
    override fun invoke(origin: String): Boolean {
        return originRegex.matches(origin)
    }
}
