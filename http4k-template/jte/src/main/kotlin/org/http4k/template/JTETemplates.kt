package org.http4k.template

import gg.jte.ContentType
import gg.jte.ContentType.Html
import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import gg.jte.resolve.DirectoryCodeResolver
import gg.jte.resolve.ResourceCodeResolver
import java.io.File


/**
 * Jade4j templating support. Use the function in the constructor to configure the instance.
 */
class JTETemplates(private val contentType: ContentType = Html) : Templates {
    override fun CachingClasspath(baseClasspathPackage: String): (ViewModel) -> String {

        val templateEngine = TemplateEngine.create(
            ResourceCodeResolver(if (baseClasspathPackage.isEmpty()) "." else baseClasspathPackage.replace('.', '/')),
            contentType
        )

        return renderUsing { templateEngine }
    }

    override fun Caching(baseTemplateDir: String): TemplateRenderer {
        val templateEngine = TemplateEngine.create(
            DirectoryCodeResolver(File(baseTemplateDir).toPath()),
            contentType
        )

        return renderUsing { templateEngine }
    }

    override fun HotReload(baseTemplateDir: String) = renderUsing {
        TemplateEngine.create(
            DirectoryCodeResolver(File(baseTemplateDir).toPath()),
            contentType
        )
    }

    private fun renderUsing(engineProvider: () -> TemplateEngine) =
        fun(viewModel: ViewModel): String {
            val templateName = viewModel.template() + ".kte"

            val templateEngine = engineProvider()

            return if (templateEngine.hasTemplate(templateName))
                StringOutput().also { templateEngine.render(templateName, viewModel, it); }.toString()
            else throw ViewNotFound(viewModel)
        }
}
