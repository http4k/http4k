package org.http4k.connect.openfeature.model

enum class Reason {
    STATIC,
    DEFAULT,
    TARGETING_MATCH,
    SPLIT,
    CACHED,
    DISABLED,
    UNKNOWN,
    STALE,
    ERROR
}
