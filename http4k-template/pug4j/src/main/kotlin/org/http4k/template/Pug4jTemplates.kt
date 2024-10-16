package org.http4k.template

import de.neuland.pug4j.Pug4J
import de.neuland.pug4j.PugConfiguration
import de.neuland.pug4j.template.FileTemplateLoader
import de.neuland.pug4j.template.TemplateLoader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.NoSuchFileException

/**
 * Pug4j templating support. Use the function in the constructor to configure the instance.
 */
class Pug4jTemplates(private val configure: PugConfiguration = PugConfiguration()) : Templates {
    override fun CachingClasspath(baseClasspathPackage: String): TemplateRenderer {
        configure.templateLoader = object : TemplateLoader {
            override fun getExtension() = "pug"

            override fun getLastModified(name: String?): Long = -1

            override fun getReader(name: String?) = InputStreamReader(
                Thread.currentThread()
                    .getContextClassLoader().getResourceAsStream("$name.${getExtension()}")
                    ?: throw NoSuchFileException(name),
                Charsets.UTF_8
            )

            override fun getBase() = "."
        }
        val basePath = if (baseClasspathPackage.isEmpty()) ""
        else baseClasspathPackage.replace('.', '/') + "/"

        return fun(viewModel: ViewModel): String {
            try {
                val template = configure.getTemplate(basePath + viewModel.template())
                return configure.renderTemplate(template, mutableMapOf<String, Any>(Pair("model", viewModel)))
            } catch (e: NoSuchFileException) {
                e.printStackTrace()
                throw ViewNotFound(viewModel)
            }
        }
    }

    override fun Caching(baseTemplateDir: String): TemplateRenderer {
        configure.templateLoader = FileTemplateLoader(baseTemplateDir + File.separator, Charsets.UTF_8)

        val cachingFun = fun(viewModel: ViewModel): String {
            val template = configure.getTemplate(viewModel.template())
            return configure.renderTemplate(template, mutableMapOf<String, Any>(Pair("model", viewModel)))
        }
        return safeRender(cachingFun)
    }

    override fun HotReload(baseTemplateDir: String) = safeRender {
        Pug4J.render(
            baseTemplateDir + File.separator + it.template(),
            mutableMapOf<String, Any>(Pair("model", it))
        )
    }

    private fun safeRender(fn: (ViewModel) -> String): (ViewModel) -> String = {
        try {
            fn(it)
        } catch (e: NoSuchFileException) {
            throw ViewNotFound(it)
        }
    }
}

