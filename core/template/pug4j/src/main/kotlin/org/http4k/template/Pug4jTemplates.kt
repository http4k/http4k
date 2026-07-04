package org.http4k.template

import de.neuland.pug4j.Pug4J
import de.neuland.pug4j.PugEngine
import de.neuland.pug4j.template.FileTemplateLoader
import de.neuland.pug4j.template.TemplateLoader
import java.io.File
import java.io.InputStreamReader
import java.io.UncheckedIOException
import java.nio.file.NoSuchFileException

/**
 * Pug4j templating support. Use the function in the constructor to configure the engine builder.
 */
class Pug4jTemplates(private val engineBuilder: PugEngine.Builder = PugEngine.builder()) : Templates {
    override fun CachingClasspath(baseClasspathPackage: String): TemplateRenderer {
        val engine = engineBuilder.templateLoader(object : TemplateLoader {
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
        ).build()
        val basePath = if (baseClasspathPackage.isEmpty()) {
            ""
        } else {
            baseClasspathPackage.replace('.', File.separatorChar) + File.separatorChar
        }

        return fun(viewModel: ViewModel): String {
            try {
                val template = engine.getTemplate(basePath + viewModel.template())
                return engine.render(template, mutableMapOf<String, Any>(Pair("model", viewModel)))
            } catch (e: UncheckedIOException) {
                throw ViewNotFound(viewModel, e)
            }
        }
    }

    override fun Caching(baseTemplateDir: String): TemplateRenderer {
        val engine = engineBuilder.templateLoader(FileTemplateLoader(baseTemplateDir + File.separator, Charsets.UTF_8)).build()
        val base = File(baseTemplateDir).canonicalFile

        val cachingFun = fun(viewModel: ViewModel): String {
            val resolved = File(base, viewModel.template()).canonicalFile
            if (resolved != base && !resolved.toPath().startsWith(base.toPath())) {
                throw NoSuchFileException(viewModel.template())
            }
            val template = engine.getTemplate(viewModel.template())
            return engine.render(template, mutableMapOf<String, Any>(Pair("model", viewModel)))
        }
        return safeRender(cachingFun)
    }

    override fun HotReload(baseTemplateDir: String) = safeRender {
        val base = File(baseTemplateDir).canonicalFile
        val resolved = File(base, it.template()).canonicalFile
        if (resolved != base && !resolved.toPath().startsWith(base.toPath())) {
            throw NoSuchFileException(it.template())
        }
        Pug4J.render(resolved.path, mutableMapOf<String, Any>(Pair("model", it)))
    }

    private fun safeRender(fn: (ViewModel) -> String): (ViewModel) -> String = {
        try {
            fn(it)
        } catch (e: NoSuchFileException) {
            throw ViewNotFound(it, e)
        }
    }
}
