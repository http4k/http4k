package org.http4k.contract

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.ResourceLoader
import org.http4k.core.StaticContent
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.then
import org.http4k.routing.Module
import org.http4k.routing.Router

class StaticModule(basePath: BasePath,
                   resourceLoader: ResourceLoader = ResourceLoader.Classpath("/"),
                   moduleFilter: Filter = Filter { it },
                   vararg extraPairs: Pair<String, ContentType>) : Module {

    private val staticContent = moduleFilter.then(StaticContent(basePath.toString(), resourceLoader, *extraPairs))

    override fun toRouter(): Router = {
        staticContent(it).let { if (it.status != NOT_FOUND) { _: Request -> it } else null }
    }
}
