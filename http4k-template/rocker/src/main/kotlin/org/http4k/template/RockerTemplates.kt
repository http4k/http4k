package org.http4k.template

import com.fizzed.rocker.RockerModel
import com.fizzed.rocker.runtime.DefaultRockerModel
import com.fizzed.rocker.runtime.RockerRuntime

/**
 * Use this class as the extension class for all Template classes in the Rocker generation step
 */
abstract class RockerViewModel : DefaultRockerModel(), ViewModel

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class RockerTemplates : Templates {

    override fun CachingClasspath(unused: String) = object : TemplateRenderer {
        override fun invoke(p1: ViewModel): String = renderIt(p1)
    }

    override fun Caching(unused: String) = object : TemplateRenderer {
        override fun invoke(p1: ViewModel): String = renderIt(p1)
    }

    override fun HotReload(unused: String) = object : TemplateRenderer {
        init {
            RockerRuntime.getInstance().isReloading = true
        }

        override fun invoke(p1: ViewModel) = renderIt(p1)
    }

    private fun renderIt(p1: ViewModel) = if (p1 is RockerModel) p1.render().toString()
    else throw ViewNotFound(p1)
}
