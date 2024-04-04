package org.http4k.routing

import org.http4k.routing.ResourceLoader.Companion.Classpath

/**
 * Serve WebJar contents from the classpath. Just install the dependencies and add this line to your routes().
 */
fun webJars() = static(Classpath("/META-INF/resources"))
