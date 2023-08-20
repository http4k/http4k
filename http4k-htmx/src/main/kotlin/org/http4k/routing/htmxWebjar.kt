package org.http4k.routing

import org.http4k.routing.ResourceLoader.Companion.Classpath

/**
 * Convenience installation of HTML Webjar
 */
fun htmxWebjar() = static(Classpath("/META-INF/resources/webjars/htmx.org/1.9.4/dist"))
