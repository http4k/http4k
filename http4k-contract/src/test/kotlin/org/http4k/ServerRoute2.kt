package org.http4k

import org.http4k.contract.BasePath
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.lens.Header
import org.http4k.routing.Router

class ServerRoute2 internal constructor(private val sbb: SBB, private val toHandler: (ExtractedParts) -> HttpHandler) {
    internal val nonBodyParams = sbb.desc.core.requestParams.plus(sbb.pathDef.pathLenses).flatMap { it }

    internal val jsonRequest: Request? = sbb.desc.core.request?.let { if (Header.Common.CONTENT_TYPE(it) == ContentType.APPLICATION_JSON) it else null }

    internal val tags = sbb.desc.core.tags.toSet().sortedBy { it.name }

    internal fun router(contractRoot: BasePath): Router = sbb.toRouter(contractRoot, toHandler)

    internal fun describeFor(contractRoot: BasePath): String = sbb.pathDef.describe(contractRoot)
}