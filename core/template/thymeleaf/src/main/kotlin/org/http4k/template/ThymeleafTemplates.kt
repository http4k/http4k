package org.http4k.template

import org.thymeleaf.ITemplateEngine
import org.thymeleaf.TemplateEngine
import org.thymeleaf.cache.StandardCacheManager
import org.thymeleaf.context.Context
import org.thymeleaf.exceptions.TemplateInputException
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.FileTemplateResolver
import java.io.File
import java.io.FileNotFoundException

class ThymeleafTemplates(private val configure: (TemplateEngine) -> TemplateEngine = { it },
                         private val classLoader: ClassLoader = ClassLoader.getSystemClassLoader()) : Templates {
    override fun CachingClasspath(baseClasspathPackage: String): TemplateRenderer =
        ThymeleafTemplateRenderer(configure(TemplateEngine().apply {
            setTemplateResolver(ClassLoaderTemplateResolver(classLoader).apply {
                templateMode = TemplateMode.HTML
                prefix = if (baseClasspathPackage.isEmpty()) "" else baseClasspathPackage.replace('.', File.separatorChar) + File.separatorChar
                suffix = ".html"
            })
        }))

    override fun Caching(baseTemplateDir: String): TemplateRenderer =
        ThymeleafTemplateRenderer(configure(TemplateEngine().apply {
            setTemplateResolver(FileTemplateResolver().apply {
                templateMode = TemplateMode.HTML
                prefix = "$baseTemplateDir/"
                suffix = ".html"
            })
        }))

    override fun HotReload(baseTemplateDir: String): TemplateRenderer =
        ThymeleafTemplateRenderer(configure(TemplateEngine().apply {
            cacheManager = StandardCacheManager().apply {
                templateCacheMaxSize = 0
            }
            setTemplateResolver(FileTemplateResolver().apply {
                templateMode = TemplateMode.HTML
                prefix = "$baseTemplateDir/"
                suffix = ".html"
            })
        }))

    private class ThymeleafTemplateRenderer(private val engine: ITemplateEngine) : TemplateRenderer {
        override fun invoke(viewModel: ViewModel): String = try {
            val templateSpec = viewModel.template()
            val separatorPos = templateSpec.indexOf("::")
            val template = if (separatorPos > 0) templateSpec.substring(0, separatorPos) else templateSpec
            val templateSelector = if (separatorPos > 0) setOf(templateSpec.substring(separatorPos + 2)) else null
            engine.process(template, templateSelector, Context().apply {
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
