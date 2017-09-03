package org.http4k.multipart.part

class Parts(val partMap: Map<String, List<Part>>) : AutoCloseable {
    override fun close() {
        partMap.values
            .flatMap { it }
            .forEach { it.close() }
    }
}
