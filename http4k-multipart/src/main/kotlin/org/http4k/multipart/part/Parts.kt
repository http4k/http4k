package org.http4k.multipart.part

import java.util.*

class Parts(partMap: Map<String, List<Part>>) : AutoCloseable {
    val partMap: Map<String, List<Part>> = Collections.unmodifiableMap(partMap)

    override fun close() {
        partMap.values
            .flatMap { it }
            .forEach { it.close() }

    }
}
