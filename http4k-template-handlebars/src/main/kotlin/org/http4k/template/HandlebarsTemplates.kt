package org.http4k.template

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.github.jknack.handlebars.io.CompositeTemplateLoader
import com.github.jknack.handlebars.io.FileTemplateLoader
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap

/**
 * Handlebars templating support. Use the function in the constructor to configure the instance.
 */
class HandlebarsTemplates(private val configure: (Handlebars) -> Handlebars = { it }) : Templates {
    override fun CachingClasspath(baseClasspathPackage: String) = object : TemplateRenderer {
        private val classToTemplate = ConcurrentHashMap<Pair<Class<*>, String>, Template>()
        private val handlebars = configure(Handlebars(ClassPathTemplateLoader("/" + baseClasspathPackage.replace('.', '/'))))

        override fun invoke(viewModel: ViewModel) =
            safeRender {
                classToTemplate.getOrPut(it.javaClass to it.template()) { handlebars.compile(it.template()) }.apply(it)
            }(viewModel)
    }

    override fun Caching(baseTemplateDir: String) = object : TemplateRenderer {
        private val classToTemplate = ConcurrentHashMap<Pair<Class<*>, String>, Template>()
        private val handlebars = configure(Handlebars(FileTemplateLoader(File(baseTemplateDir))))

        override fun invoke(viewModel: ViewModel) =
            safeRender {
                classToTemplate.getOrPut(it.javaClass to it.template()) { handlebars.compile(it.template()) }.apply(it)
            }(viewModel)
    }

    override fun HotReload(baseTemplateDir: String): TemplateRenderer = object : TemplateRenderer {
        val handlebars = configure(Handlebars(FileTemplateLoader(File(baseTemplateDir))))
        override fun invoke(viewModel: ViewModel): String =
            safeRender {
                handlebars.compile(it.template()).apply(it)
            }(viewModel)
    }


    /**
     * Hot-reloads (no-caching) templates from a file path
     *
     * @param firstBaseDir the first dir to load templates from
     * @param secondBaseDir the second dir to load templates from
     * @param rest the rest
     */
    fun HotReload(firstBaseDir: String, secondBaseDir: String, vararg rest: String): TemplateRenderer = object : TemplateRenderer {
        val loaders = listOf(firstBaseDir, secondBaseDir, *rest).map { FileTemplateLoader(File(it)) }
        val handlebars = configure(Handlebars(CompositeTemplateLoader(*loaders.toTypedArray())))
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
