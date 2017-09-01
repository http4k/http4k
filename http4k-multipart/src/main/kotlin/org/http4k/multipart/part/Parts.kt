package org.http4k.multipart.part

import java.io.IOException
import java.util.*

class Parts(partMap: Map<String, List<Part>>) : AutoCloseable {
    val partMap: Map<String, List<Part>> = Collections.unmodifiableMap(partMap)

    @Throws(IOException::class)
    override fun close() {
        partMap.values
            .flatMap { it }
            .forEach { it.close() }

    }
}
