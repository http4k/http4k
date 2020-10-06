package org.http4k.template

import org.http4k.template.dust.Dust
import org.http4k.template.dust.TemplateLoader
import org.http4k.template.dust.loadFromFilesIn
import org.http4k.template.dust.loadFromResourcesIn
import java.net.URL

class DustTemplates(
    private val precachePoolSize: Int = 0,
    private val dustPluginScripts: List<URL> = emptyList()

) : Templates {

    override fun Caching(baseTemplateDir: String): TemplateRenderer =
        dust4Http4K(true, loadFromFilesIn(baseTemplateDir))

    override fun CachingClasspath(baseClasspathPackage: String): TemplateRenderer =
        dust4Http4K(true, loadFromResourcesIn(baseClasspathPackage))

    override fun HotReload(baseTemplateDir: String): TemplateRenderer =
        dust4Http4K(false, loadFromFilesIn(baseTemplateDir))

    private fun dust4Http4K(cacheTemplates: Boolean, loader: TemplateLoader): (ViewModel) -> String {
        val dust = Dust(
            cacheTemplates = cacheTemplates,
            precachePoolSize = precachePoolSize,
            dustPluginScripts = dustPluginScripts,
            loader = loader
        )

        return fun(viewModel: ViewModel) =
            dust.withTemplates {
                it.expandTemplate(
                    viewModel.template(), viewModel,
                    onMissingTemplate = { throw ViewNotFound(viewModel) }
                )
            }
    }
}
