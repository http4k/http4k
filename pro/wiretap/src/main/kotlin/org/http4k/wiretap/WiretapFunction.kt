/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap

import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.routing.RoutingHttpHandler
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer

interface WiretapFunction {
    fun http(elements: DatastarElementRenderer, html: TemplateRenderer): RoutingHttpHandler
    fun mcp(): ServerCapability
}
