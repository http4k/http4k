package org.http4k.format

/**
 * Determines if the marshaller should be lenient when parsing unknown fields
 */
enum class StrictnessMode {
    Lenient, FailOnUnknown
}
