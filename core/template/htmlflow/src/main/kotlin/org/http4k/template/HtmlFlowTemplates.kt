package org.http4k.template

import htmlflow.HtmlFlow
import htmlflow.viewloader.ClasspathLoader

class HtmlFlowTemplates: Templates {

    private val loader = ClasspathLoader(ViewModel::class.java)

    private val factory = HtmlFlow.ViewFactory
        .builder()
        .threadSafe(true)
        .preEncoding(true)

    override fun CachingClasspath(baseClasspathPackage: String): TemplateRenderer {
        return renderer(baseClasspathPackage, hotReload = false)
    }

    fun HotReloadClasspath(baseClasspathPackage: String = ""): TemplateRenderer {
        return renderer(baseClasspathPackage, hotReload = true)
    }

    override fun Caching(baseTemplateDir: String): TemplateRenderer {
        val basePackage = convertPathToPackage(baseTemplateDir)
        return renderer(basePackage, hotReload = false)
    }

    override fun HotReload(baseTemplateDir: String): TemplateRenderer {
        val basePackage = convertPathToPackage(baseTemplateDir)
        return renderer(basePackage, hotReload = true)
    }

    private fun renderer(basePackage: String, hotReload: Boolean): TemplateRenderer {
        val renderer = loader.createRenderer(basePackage, hotReload, factory.preEncoding(!hotReload).build())
        return { model ->
            try {
                renderer(model)
            } catch (e: htmlflow.viewloader.ViewNotFound) {
                throw ViewNotFound(model)
            }
        }
    }

    private fun convertPathToPackage(path: String): String {
        return path
            .replace(Regex("^(src[/\\\\](main|test)[/\\\\]kotlin([/\\\\])?)"), "")
            .replace(Regex("[/\\\\]"), ".")
            .trim('.')
    }
}
