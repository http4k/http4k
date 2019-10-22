package org.http4k.template

import de.neuland.jade4j.Jade4J
import de.neuland.jade4j.JadeConfiguration
import de.neuland.jade4j.template.ClasspathTemplateLoader
import de.neuland.jade4j.template.FileTemplateLoader
import de.neuland.jade4j.template.JadeTemplate
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import java.util.concurrent.ConcurrentHashMap

/**
 * Jade4j templating support. Use the function in the constructor to configure the instance.
 */
class Jade4jTemplates(private val configure: JadeConfiguration = JadeConfiguration()) : Templates {
    override fun CachingClasspath(baseClasspathPackage: String): TemplateRenderer {
        configure.templateLoader = ClasspathTemplateLoader()
        val baseClasspathPackagePath = File.separator + baseClasspathPackage.replace(".", File.separator) + File.separator

        return fun(viewModel: ViewModel): String {
            try {
                val template = configure.getTemplate(baseClasspathPackagePath + viewModel.template())
                return configure.renderTemplate(template, mutableMapOf<String, Any>( Pair("model", viewModel) ))
            }  catch (e: NullPointerException) {
                throw ViewNotFound(viewModel)
            }
        }
    }

    override fun Caching(baseTemplateDir: String): TemplateRenderer {
        val loader = FileTemplateLoader(baseTemplateDir + File.separator, "UTF-8");
        configure.templateLoader = loader

        val cachingFun = fun(viewModel: ViewModel): String {
            val template = configure.getTemplate(viewModel.template())
            return configure.renderTemplate(template, mutableMapOf<String, Any>( Pair("model", viewModel) ))
        }
        return safeRender(cachingFun)
    }

    override fun HotReload(baseTemplateDir: String): TemplateRenderer {
        val hotReloadFun = fun(viewModel: ViewModel) = Jade4J.render(
                baseTemplateDir + File.separator + viewModel.template(),
                mutableMapOf<String, Any>( Pair("model", viewModel) )
        )
        return safeRender(hotReloadFun)
    }

    private fun safeRender(fn: (ViewModel) -> String): (ViewModel) -> String = {
        try {
            fn(it)
        } catch (e: FileNotFoundException) {
            throw ViewNotFound(it)
        }
    }
}
