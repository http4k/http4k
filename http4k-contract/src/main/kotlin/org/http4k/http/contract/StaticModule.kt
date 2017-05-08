package org.http4k.http.contract

import org.http4k.http.core.Filter
import org.http4k.http.core.Request
import org.http4k.http.core.ResourceLoader
import org.http4k.http.core.StaticContent
import org.http4k.http.core.Status.Companion.NOT_FOUND
import org.http4k.http.core.then

class StaticModule(basePath: BasePath,
                   resourceLoader: ResourceLoader = ResourceLoader.Classpath("/"),
                   moduleFilter: Filter = Filter { it }) : Module {

    private val staticContent = moduleFilter.then(StaticContent(basePath.toString(), resourceLoader))

    override fun toRouter(): Router = {
        staticContent(it).let { if (it.status != NOT_FOUND) { _: Request -> it } else null }
    }
}
