package org.http4k.core

/**
 * A content range (e.g. media type range, language range, etc.) weighted by priority
 */
data class Weighted<T>(val range: T, val priority: Double) {
    val contentType get() = range // backward compatibility
}

/**
 * A convenience operator for creating a Weighted<T> programmatically.
 */
infix fun <T> T.q(q: Double): Weighted<T> = Weighted(this, q)

/**
 * A weighted list of content ranges of type T.
 */
data class PriorityList<T>(val ranges: List<Weighted<T>>) {
    companion object
}

/**
 * Constructs a PriorityList from a fixed set of weighted ranges and
 * ensures it is sorted by descending weight.
 */
fun <T> PriorityList(vararg values: Weighted<T>): PriorityList<T> =
    PriorityList(values.toList().sortedByDescending { it.priority })

/**
 * Selects the client's preferred option from a set of options offered
 * by the server, matching a priority list against the id of the options.
 *
 * Precondition: the Priority list is sorted in descending order of weight.
 * This is guaranteed by the header parser and the function that creates a
 * fixed PriorityList.
 *
 * @param offered the list of options offered
 * @param match a predicate that reports whether a range selects an option identified by its id
 * @param by a function that returns an option's id
 */
inline fun <Range, Option: Any, OptionId> PriorityList<Range>.preferred(
    offered: List<Option>,
    match: (Range, OptionId) -> Boolean,
    by: (Option)->OptionId
): Option? {
    ranges.forEach { qr ->
        offered.forEach { o ->
            if (match(qr.range, by(o))) return o
        }
    }
    return null
}

/**
 * Selects the client's preferred option from a set of options offered
 * by the server, when the range is a SimpleRange<Option>.
 *
 * This is useful for implementing content negotiation in the application,
 * rather than in request routing.
 */
fun <Range, Option: Any> PriorityList<Range>.preferred(offered: List<Option>): Option?
where Range : SimpleRange<Option> =
    preferred(offered, {r,o->r.matches(o)}, {it})


internal typealias HeaderParams = Map<String, String>
internal val NoParams = emptyMap<String, String>()


/**
 * Parses a PriorityList from a header value.
 *
 * Follows RFC 9110: "Previous specifications allowed additional extension parameters to appear after
 * the weight parameter. The accept extension grammar (accept-params, accept-ext) has been removed
 * because it had a complicated definition, was not being used in practice, and is more easily
 * deployed through new header fields. Senders using weights SHOULD send “q” last (after all
 * media-range parameters). Recipients SHOULD process any parameter named “q” as weight,
 * regardless of parameter ordering." https://www.rfc-editor.org/rfc/rfc9110.html#name-accept
 */
fun <T> PriorityList.Companion.fromHeader(s: String, parseValue: (String, HeaderParams) -> T) =
    s.split(',')
        .map(String::trim)
        .map { part ->
            val subparts = part.split(";").map(String::trim)
            val params = subparts.drop(1)
                .map { str -> str.split("=", limit = 2).map(String::trim) }
                .associate { kv -> kv.first() to kv.getOrElse(1, { "" }) }
            Weighted(
                range = parseValue(subparts.first(), params - "q"),
                priority = params["q"]?.toDouble() ?: 1.0 // RFC 9110, section 12.4.2
            )
        }
        .sortedByDescending { it.priority }
        .let(::PriorityList)


fun <T> PriorityList<T>.toHeader(rangeToHeader: (T) -> Pair<String, HeaderParams>) =
    ranges.joinToString(separator = ",") { (value, q) ->
        val (token, params) = rangeToHeader(value)
        token +
            params.entries.joinToString(";") { (key, value) -> "$key=$value" } +
            (if (q == 1.0) "" else ";q=$q") // RFC 9110, section 12.4.2
    }

/**
 * A SimpleRange is either a value of some type, T, or a wildcard,
 * represented in headers as "*", and matching any value of T.
 * The value is constrained to being represented in HTTP headers
 * by a single token, with no parameters.
 *
 * This is suitable for headers Accept-Charset, Accept-Encoding, and
 * Accept-Language, but not Accept.
 */
sealed interface SimpleRange<in T> {
    fun matches(t: T): Boolean
    
    companion object
}

data class Exactly<T>(val value: T) : SimpleRange<T> {
    override fun matches(t: T) = t == value
}

data object Wildcard : SimpleRange<Any?> {
    override fun matches(t: Any?) = true
}

fun <T> SimpleRange.Companion.fromHeader(
    s: String,
    valueFromHeader: (String) -> T
): SimpleRange<T> {
    return when (s) {
        "*" -> Wildcard
        else -> Exactly(valueFromHeader(s))
    }
}

fun <T> SimpleRange<T>.forHeader(valueForHeader: (T) -> String) =
    Pair(
        when (this) {
            Wildcard -> "*"
            is Exactly<T> -> valueForHeader(value)
        },
        NoParams
    )


fun <OptionId> PriorityList.Companion.fromSimpleRangeHeader(
    string: String,
    optionFromHeader: (String) -> OptionId
): PriorityList<SimpleRange<OptionId>> =
    PriorityList.fromHeader(string) { s, _ -> SimpleRange.fromHeader(s, optionFromHeader) }


fun <OptionId> PriorityList<SimpleRange<OptionId>>.toSimpleRangeHeader(
    optionForHeader: (OptionId)-> String
): String =
    toHeader { range : SimpleRange<OptionId> -> range.forHeader(optionForHeader) }


