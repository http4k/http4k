@file:Suppress("DEPRECATION")

package settings

import org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4
import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import java.io.File
import java.util.Locale.getDefault

data class Template(val name: String, val description: String, val value: String, val variables: List<String> = emptyList()) : ViewModel

data class TemplateSet(val name: String, val templates: List<Template>) : ViewModel {
    val cleaned = templates.map { it.copy(value = unescapeHtml4(it.value)
        .replace("org.http4k.core.", "")
        .replace("            ", " ")
        .replace("\n", "")) }
}

data class Readme(val templateSets: List<TemplateSet>) : ViewModel

fun File.writeLiveTemplates() {

    val standard = TemplateSet("Standard",
        listOf(
            Template("fil", "http4k Filter", """org.http4k.core.Filter { next -${"&"}gt;${"&"}#10;            {${"&"}#10;                ${"$"}content${"$"}${"&"}#10;                next(it)${"&"}#10;            }${"&"}#10;        }""", listOf("content")),
            Template("hh", "http4k Handler", """{ req: org.http4k.core.Request -&gt; org.http4k.core.Response(org.http4k.core.Status.OK) }""", listOf("content"))
        )
    )

    val methods = TemplateSet("Request",
        Method.entries.map {
            Template(
                it.name.lowercase(getDefault()), "http4k ${it.name} Request",
                """org.http4k.core.Request(org.http4k.core.Method.${it.name}, &quot;${"$"}path${"$"}&quot;)""",
                listOf("path"))
        }
    )

    fun Status.statusTemplate(): Template = Template(code.toString(), "http4k $code Response", "org.http4k.core.Response(org.http4k.core.Status.Companion.${
        description.uppercase(getDefault()).replace(' ', '_')})")

    val statii = TemplateSet("Response", Status.serverValues
        .distinct()
        .sortedBy { it.code }
        .map(Status::statusTemplate)
    )

    val templates = HandlebarsTemplates().Caching("src/test/resources")

    File(this, "README.md").apply {
        delete()
        writeText(templates(Readme(listOf(standard, methods, statii))))
    }

    File(this, "templates").apply {
        mkdirs()
        File(this, "http4k.xml").apply {
            delete()
            writeText(templates(TemplateSet("all", standard.templates + methods.templates + statii.templates)))
        }
    }
}
