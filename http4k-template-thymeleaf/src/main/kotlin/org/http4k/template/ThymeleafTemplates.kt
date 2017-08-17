package org.http4k.template

import org.thymeleaf.ITemplateEngine
import org.thymeleaf.TemplateEngine
import org.thymeleaf.cache.StandardCacheManager
import org.thymeleaf.context.Context
import org.thymeleaf.exceptions.TemplateInputException
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.FileTemplateResolver
import java.io.FileNotFoundException

class ThymeleafTemplates(private val configure: (TemplateEngine) -> TemplateEngine = { it }) : Templates {
    override fun CachingClasspath(baseClasspathPackage: String): TemplateRenderer {
        val templateEngine = TemplateEngine()
        val loader = ClassLoaderTemplateResolver()
        loader.prefix = if (baseClasspathPackage.isEmpty()) "" else baseClasspathPackage.replace('.', '/') + "/"
        templateEngine.setTemplateResolver(loader)
        return ThymeleafTemplateRenderer(configure(templateEngine))
    }

    override fun Caching(baseTemplateDir: String): TemplateRenderer {
        val templateEngine = TemplateEngine()
        val loader = FileTemplateResolver()
        loader.prefix = baseTemplateDir + "/"
        templateEngine.setTemplateResolver(loader)
        return ThymeleafTemplateRenderer(configure(templateEngine))
    }

    override fun HotReload(baseTemplateDir: String): TemplateRenderer {
        val templateEngine = TemplateEngine()
        val loader = FileTemplateResolver()
        loader.prefix = baseTemplateDir + "/"
        val cache = StandardCacheManager()
        cache.templateCacheMaxSize = 0
        templateEngine.cacheManager = cache
        templateEngine.setTemplateResolver(loader)
        return ThymeleafTemplateRenderer(configure(templateEngine))
    }

    private class ThymeleafTemplateRenderer(private val engine: ITemplateEngine) : TemplateRenderer {
        override fun invoke(viewModel: ViewModel): String {
            val context = Context()
            context.setVariable("model", viewModel)
            return try {
                engine.process(viewModel.template(), context)
            } catch (e: TemplateInputException) {
                e.printStackTrace()
                when (e.cause) {
                    is FileNotFoundException -> throw ViewNotFound(viewModel)
                    else -> throw e
                }
            }
        }
    }
}