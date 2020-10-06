package org.http4k.template.dust

import jdk.nashorn.api.scripting.JSObject
import org.apache.commons.pool2.BasePooledObjectFactory
import org.apache.commons.pool2.impl.DefaultPooledObject
import org.apache.commons.pool2.impl.GenericObjectPool
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import java.io.StringWriter
import java.net.URL
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

typealias TemplateLoader = (templateName: String) -> String?

interface TemplateExpansion {
    fun expandTemplate(
        templateName: String,
        params: Any,
        onMissingTemplate: (templateName: String) -> Nothing = ::missingTemplateIllegalArgument
    ): String
}

interface TemplateExpansionService : AutoCloseable, TemplateExpansion

private object TEMPLATE_NOT_FOUND

private fun missingTemplateIllegalArgument(templateName: String): Nothing = throw IllegalArgumentException("template $templateName not found")

private fun ScriptEngine.eval(srcUrl: URL) {
    eval(srcUrl.readText())
}

// Must only be used on one thread.
private class SingleThreadedDust(
    private val js: ScriptEngine,
    private val cacheTemplates: Boolean = true,
    private val dustPluginScripts: List<URL>,
    private val notifyOnClosed: (SingleThreadedDust) -> Unit

) : TemplateExpansionService {

    private val dust: JSObject = run {
        js.eval(javaClass.getResource("dust-full-2.7.5.js"))
        dustPluginScripts.forEach(js::eval)
        js.eval(
            //language=JavaScript
            """
            // This lets Dust iterate over Java collections.
            dust.isArray = function(o) {
                return Array.isArray(o) || o instanceof java.util.List;
            };
            dust.config.cache = $cacheTemplates;
            dust.onLoad = function(templateName, callback) {
                var template = loader.invoke(templateName);
                if (template === null) {
                    callback(TEMPLATE_NOT_FOUND, null)
                } else {
                    callback(null, template);
                }
            }
            """
        )

        js["dust"] as? JSObject ?: throw IllegalStateException("could not initialise Dust")
    }

    override fun close() {
        notifyOnClosed(this)
    }

    override fun expandTemplate(
        templateName: String,
        params: Any,
        onMissingTemplate: (templateName: String) -> Nothing
    ): String {
        val writer = StringWriter()
        var error: Any? = null

        val bindings = SimpleBindings(
            mapOf(
                "dust" to dust,
                "templateName" to templateName,
                "templateParams" to params,
                "writer" to writer,
                "reportError" to { e: Any -> error = e }
            )
        )

        js.eval(
            //language=JavaScript
            """
            dust.render(templateName, templateParams, function(err, result) {
                if (err) {
                    reportError.invoke(err);
                } else {
                    writer.write(result, 0, result.length);
                }
            });
            """,
            bindings
        )

        return when (error) {
            null -> writer.toString()
            TEMPLATE_NOT_FOUND -> onMissingTemplate(templateName)
            else -> throw IllegalStateException(error.toString())
        }
    }
}

class Dust(
    private val cacheTemplates: Boolean,
    private val precachePoolSize: Int,
    private val dustPluginScripts: List<URL>,
    loader: TemplateLoader
) {
    private val scriptEngineManager = ScriptEngineManager().apply {
        bindings = SimpleBindings(
            mapOf(
                "loader" to loader,
                "TEMPLATE_NOT_FOUND" to TEMPLATE_NOT_FOUND
            )
        )
    }

    private val pool = GenericObjectPool(
        object : BasePooledObjectFactory<SingleThreadedDust>() {
            override fun create(): SingleThreadedDust = SingleThreadedDust(
                js = scriptEngineManager.getEngineByName("nashorn"),
                cacheTemplates = cacheTemplates,
                dustPluginScripts = dustPluginScripts,
                notifyOnClosed = { returnDustEngine(it) }
            )

            override fun wrap(obj: SingleThreadedDust) = DefaultPooledObject(obj)
        },
        GenericObjectPoolConfig<SingleThreadedDust>().apply {
            minIdle = precachePoolSize
        }
    )

    private fun returnDustEngine(dustEngine: SingleThreadedDust) {
        pool.returnObject(dustEngine)
    }

    fun openTemplates(): TemplateExpansionService = pool.borrowObject()

    inline fun <T> withTemplates(block: (TemplateExpansion) -> T): T = openTemplates().use(block)
}
