package org.http4k.template

import org.thymeleaf.ITemplateEngine
import org.thymeleaf.TemplateEngine
import org.thymeleaf.cache.StandardCacheManager
import org.thymeleaf.context.Context
import org.thymeleaf.exceptions.TemplateInputException
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.FileTemplateResolver
import java.io.FileNotFoundException

class ThymeleafTemplates(private val configure: (TemplateEngine) -> TemplateEngine = { it },
                         private val classLoader: ClassLoader = ClassLoader.getSystemClassLoader()) : Templates {
    override fun CachingClasspath(baseClasspathPackage: String): TemplateRenderer =
        ThymeleafTemplateRenderer(configure(TemplateEngine().apply {
            setTemplateResolver(ClassLoaderTemplateResolver(classLoader).apply {
                prefix = if (baseClasspathPackage.isEmpty()) "" else baseClasspathPackage.replace('.', '/') + "/"
            })
        }))

    override fun Caching(baseTemplateDir: String): TemplateRenderer =
        ThymeleafTemplateRenderer(configure(TemplateEngine().apply {
            setTemplateResolver(FileTemplateResolver().apply {
                prefix = "$baseTemplateDir/"
            })
        }))

    override fun HotReload(baseTemplateDir: String): TemplateRenderer =
        ThymeleafTemplateRenderer(configure(TemplateEngine().apply {
            cacheManager = StandardCacheManager().apply {
                templateCacheMaxSize = 0
            }
            setTemplateResolver(FileTemplateResolver().apply {
                prefix = "$baseTemplateDir/"
            })
        }))

    private class ThymeleafTemplateRenderer(private val engine: ITemplateEngine) : TemplateRenderer {
        override fun invoke(viewModel: ViewModel): String = try {
            engine.process(viewModel.template(), Context().apply {
                setVariable("model", viewModel)
            })
        } catch (e: TemplateInputException) {
            when (e.cause) {
                is FileNotFoundException -> throw ViewNotFound(viewModel)
                else -> throw e
            }
        }
    }
}