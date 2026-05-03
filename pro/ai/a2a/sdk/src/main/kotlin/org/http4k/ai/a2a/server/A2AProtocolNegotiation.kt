/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server

import org.http4k.ai.a2a.model.AgentCapabilities
import org.http4k.ai.a2a.protocol.ProtocolVersion
import org.http4k.ai.a2a.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.lens.Header
import org.http4k.lens.value

private val a2aVersionHeader = Header.value(ProtocolVersion).optional("A2A-Version")
private val a2aExtensionsHeader = Header.multi.defaulted("A2A-Extensions", emptyList())

fun A2AProtocolNegotiation(
    capabilities: AgentCapabilities,
    supportedVersions: Set<ProtocolVersion> = setOf(LATEST_VERSION)
): Filter = Filter { next ->
    { request ->
        val version = a2aVersionHeader(request)

        when {
            version != null && version !in supportedVersions -> Response(BAD_REQUEST)
            else -> {
                val clientExtensions = a2aExtensionsHeader(request)

                val requiredExtensions = capabilities.extensions
                    ?.filter { it.required }
                    ?.map { it.uri.toString() }
                    ?: emptyList()

                val missingRequired = requiredExtensions.filterNot { it in clientExtensions }
                when {
                    missingRequired.isNotEmpty() -> Response(BAD_REQUEST)
                    else -> next(request)
                }
            }
        }
    }
}
