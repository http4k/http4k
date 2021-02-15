package org.http4k.util

internal inline fun <reified T> loadMetaResource(resource: String) =
    T::class.java.getResourceAsStream("/META-INF/${T::class.java.`package`.name.replace('.', '/')}/$resource")
        ?: throw IllegalStateException("Could not find '$resource' inside META-INF. If using Shadow JAR, add mergeServiceFiles() to the configuration")
