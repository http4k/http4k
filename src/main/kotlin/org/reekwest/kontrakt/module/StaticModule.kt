package org.reekwest.kontrakt.module

import org.reekwest.http.core.Filter
import org.reekwest.http.core.Request
import org.reekwest.http.core.ResourceLoader
import org.reekwest.http.core.StaticContent
import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.then

class StaticModule(basePath: BasePath,
                   resourceLoader: ResourceLoader = ResourceLoader.Classpath("/"),
                   moduleFilter: Filter = Filter { it }) : Module {

    private val staticContent = moduleFilter.then(StaticContent(basePath.toString(), resourceLoader))

    override fun toRouter(): Router = {
        staticContent(it).let { if (it.status != NOT_FOUND) { _: Request -> it } else null }
    }
}
