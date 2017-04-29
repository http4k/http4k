package org.reekwest.http.templates

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.github.jknack.handlebars.io.FileTemplateLoader
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap


object HandlebarsTemplates : Templates {
    override fun CachingClasspath(baseClasspathPackage: String) = object : TemplateRenderer {
        private val classToTemplate = ConcurrentHashMap<Class<*>, Template>()
        private val handlebars = Handlebars(ClassPathTemplateLoader(baseClasspathPackage))

        override fun toBody(view: View) =
            safeRender {
                view ->
                classToTemplate.getOrPut(view.javaClass, { handlebars.compile(view.template) }).apply(view)
            }(view)
    }

    override fun Caching(baseTemplateDir: String) = object : TemplateRenderer {
        private val classToTemplate = ConcurrentHashMap<Class<*>, Template>()
        private val handlebars = Handlebars(FileTemplateLoader(File(baseTemplateDir)))

        override fun toBody(view: View) =
            safeRender {
                view ->
                classToTemplate.getOrPut(view.javaClass, { handlebars.compile(view.template) }).apply(view)
            }(view)

    }

    override fun HotReload(baseTemplateDir: String): TemplateRenderer = object : TemplateRenderer {
        val handlebars = Handlebars(FileTemplateLoader(File(baseTemplateDir)))
        override fun toBody(view: View): String =
            safeRender {
                view ->
                handlebars.compile(view.template).apply(view)
            }(view)
    }

    private fun safeRender(fn: (View) -> String): (View) -> String = {
        try {
            fn(it)
        } catch (e: FileNotFoundException) {
            throw ViewNotFound(it)
        }
    }

}
