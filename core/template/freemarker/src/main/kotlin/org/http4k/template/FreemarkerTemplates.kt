package org.http4k.template

import freemarker.cache.ClassTemplateLoader
import freemarker.cache.FileTemplateLoader
import freemarker.template.Configuration
import freemarker.template.TemplateNotFoundException
import java.io.File
import java.io.StringWriter

class FreemarkerTemplates(
    private val configuration: Configuration,
    private val classLoader: ClassLoader = ClassLoader.getSystemClassLoader()
) : Templates {

    @Deprecated("Use the main constructor that takes the Freemarker configuration. This is for compatibility with changing Freemarker syntax versions")
    constructor(
        configure: (Configuration) -> Configuration = { Configuration(Configuration.getVersion()) },
        classLoader: ClassLoader = ClassLoader.getSystemClassLoader()
    ) : this(configure(Configuration(Configuration.getVersion())), classLoader)

    override fun CachingClasspath(baseClasspathPackage: String): TemplateRenderer =
        FreemarkerTemplateResolver(configuration.apply {
            templateLoader = ClassTemplateLoader(classLoader, baseClasspathPackage.replace(".", "/"))
        })

    override fun Caching(baseTemplateDir: String): TemplateRenderer =
        FreemarkerTemplateResolver(configuration.apply {
            templateLoader = FileTemplateLoader(File(baseTemplateDir))
        })

    override fun HotReload(baseTemplateDir: String): TemplateRenderer =
        FreemarkerTemplateResolver(configuration.apply {
            templateUpdateDelayMilliseconds = 0
            templateLoader = FileTemplateLoader(File(baseTemplateDir))
        })

    private class FreemarkerTemplateResolver(private val configuration: Configuration) : TemplateRenderer {
        override fun invoke(viewModel: ViewModel): String = try {
            val stringWriter = StringWriter()
            val template = configuration.getTemplate(viewModel.template())
            template.process(viewModel, stringWriter)
            stringWriter.toString()
        } catch (e: TemplateNotFoundException) {
            throw ViewNotFound(viewModel, e)
        }
    }
}
