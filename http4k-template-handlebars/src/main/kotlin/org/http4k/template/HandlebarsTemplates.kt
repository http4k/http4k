package org.http4k.template

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.github.jknack.handlebars.io.FileTemplateLoader
import org.http4k.http.templates.TemplateRenderer
import org.http4k.http.templates.Templates
import org.http4k.http.templates.ViewModel
import org.http4k.http.templates.ViewNotFound
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap

/**
 * Handlebars templating support. Use the function in the constructor to configure the instance.
 */
class HandlebarsTemplates(private val configure: (Handlebars) -> Handlebars = { it }) : Templates {
    override fun CachingClasspath(baseClasspathPackage: String) = object : TemplateRenderer {
        private val classToTemplate = ConcurrentHashMap<Class<*>, Template>()
        private val handlebars = configure(Handlebars(ClassPathTemplateLoader(baseClasspathPackage)))

        override fun invoke(viewModel: ViewModel) =
            safeRender {
                classToTemplate.getOrPut(it.javaClass, { handlebars.compile(it.template()) }).apply(it)
            }(viewModel)
    }

    override fun Caching(baseTemplateDir: String) = object : TemplateRenderer {
        private val classToTemplate = ConcurrentHashMap<Class<*>, Template>()
        private val handlebars = configure(Handlebars(FileTemplateLoader(File(baseTemplateDir))))

        override fun invoke(viewModel: ViewModel) =
            safeRender {
                classToTemplate.getOrPut(it.javaClass, { handlebars.compile(it.template()) }).apply(it)
            }(viewModel)

    }

    override fun HotReload(baseTemplateDir: String): TemplateRenderer = object : TemplateRenderer {
        val handlebars = configure(Handlebars(FileTemplateLoader(File(baseTemplateDir))))
        override fun invoke(viewModel: ViewModel): String =
            safeRender {
                handlebars.compile(it.template()).apply(it)
            }(viewModel)
    }

    private fun safeRender(fn: (ViewModel) -> String): (ViewModel) -> String = {
        try {
            fn(it)
        } catch (e: FileNotFoundException) {
            throw ViewNotFound(it)
        }
    }

}
