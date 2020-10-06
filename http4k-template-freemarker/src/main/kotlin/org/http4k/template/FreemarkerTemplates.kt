package org.http4k.template

import freemarker.cache.ClassTemplateLoader
import freemarker.cache.FileTemplateLoader
import freemarker.template.Configuration
import freemarker.template.TemplateNotFoundException
import java.io.File
import java.io.StringWriter

class FreemarkerTemplates(
    private val configure: (Configuration) -> Configuration = { it },
    private val classLoader: ClassLoader = ClassLoader.getSystemClassLoader()
) : Templates {

    override fun CachingClasspath(baseClasspathPackage: String): TemplateRenderer =
        FreemarkerTemplateResolver(
            configure(
                Configuration(Configuration.getVersion()).apply {
                    templateLoader = ClassTemplateLoader(classLoader, baseClasspathPackage.replace(".", "/"))
                }
            )
        )

    override fun Caching(baseTemplateDir: String): TemplateRenderer =
        FreemarkerTemplateResolver(
            configure(
                Configuration(Configuration.getVersion()).apply {
                    templateLoader = FileTemplateLoader(File(baseTemplateDir))
                }
            )
        )

    override fun HotReload(baseTemplateDir: String): TemplateRenderer =
        FreemarkerTemplateResolver(
            configure(
                Configuration(Configuration.getVersion()).apply {
                    templateUpdateDelayMilliseconds = 0
                    templateLoader = FileTemplateLoader(File(baseTemplateDir))
                }
            )
        )

    private class FreemarkerTemplateResolver(private val configuration: Configuration) : TemplateRenderer {
        override fun invoke(viewModel: ViewModel): String = try {
            val stringWriter = StringWriter()
            val template = configuration.getTemplate(viewModel.template())
            template.process(viewModel, stringWriter)
            stringWriter.toString()
        } catch (e: TemplateNotFoundException) {
            throw ViewNotFound(viewModel)
        }
    }
}
