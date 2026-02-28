package org.http4k.wiretap.util

import org.http4k.template.HandlebarsTemplates

fun Templates() =
    HandlebarsTemplates({ it.apply { setInfiniteLoops(true) } }).CachingClasspath()
