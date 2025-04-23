package org.http4k.a2a.protocol.messages

sealed interface HasMetadata {
    val metadata: Metadata? get() = mapOf()
}
