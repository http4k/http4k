/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

/**
 * Provides the facility to compose many capabilities together.
 */
class CapabilityPack(private vararg val bindings: ServerCapability) : ServerCapability {
    constructor(bindings: List<ServerCapability>) : this(*bindings.toTypedArray())

    override fun iterator() = bindings.iterator()
    override val name = bindings.joinToString("-") { it.name }
}

fun capabilities(vararg bindings: ServerCapability): ServerCapability = CapabilityPack(*bindings)
fun capabilities(bindings: List<ServerCapability>) = capabilities(*bindings.toTypedArray())
