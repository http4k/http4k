package org.http4k.template

import org.http4k.template.dust.Dust
import org.http4k.template.dust.TemplateLoader
import org.http4k.template.dust.loadFromFilesIn
import org.http4k.template.dust.loadFromResourcesIn

class DustTemplates(private val precache: Int = 0) : Templates {
    override fun Caching(baseTemplateDir: String): TemplateRenderer = dust4Http4K(true, loadFromFilesIn(baseTemplateDir))

    override fun CachingClasspath(baseClasspathPackage: String): TemplateRenderer = dust4Http4K(true, loadFromResourcesIn(baseClasspathPackage))

    override fun HotReload(baseTemplateDir: String): TemplateRenderer = dust4Http4K(false, loadFromFilesIn(baseTemplateDir))

    private fun dust4Http4K(cacheTemplates: Boolean, loader: TemplateLoader): (ViewModel) -> String {
        val dust = Dust(cacheTemplates = cacheTemplates, precachePoolSize = precache, loader = loader)

        return fun(viewModel: ViewModel) =
            dust.withTemplates { it.expandTemplate(viewModel.template(), viewModel,
                onMissingTemplate = { throw ViewNotFound(viewModel) })
            }
    }
}
