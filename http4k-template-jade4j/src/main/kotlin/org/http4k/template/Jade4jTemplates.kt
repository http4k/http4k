package org.http4k.template

import de.neuland.jade4j.Jade4J
import de.neuland.jade4j.JadeConfiguration
import de.neuland.jade4j.template.JadeTemplate
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap

/**
 * Jade4j templating support. Use the function in the constructor to configure the instance.
 */
class Jade4jTemplates(private val configure: (JadeConfiguration) -> JadeConfiguration = { it }) : Templates {
    override fun CachingClasspath(baseClasspathPackage: String): TemplateRenderer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun Caching(baseTemplateDir: String): TemplateRenderer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun HotReload(baseTemplateDir: String): TemplateRenderer {
        return fun(viewModel: ViewModel) = Jade4J.render(
                baseTemplateDir + File.separator + viewModel.template(),
                mutableMapOf<String, Any>()
        )
    }

    private fun safeRender(fn: (ViewModel) -> String): (ViewModel) -> String = {
        try {
            fn(it)
        } catch (e: FileNotFoundException) {
            throw ViewNotFound(it)
        }
    }
}
