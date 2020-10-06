package settings

import org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4
import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import java.io.File

data class Template(val name: String, val description: String, val value: String, val variables: List<String> = emptyList()) : ViewModel

data class TemplateSet(val name: String, val templates: List<Template>) : ViewModel {
    val cleaned = templates.map {
        it.copy(
            value = unescapeHtml4(it.value)
                .replace("org.http4k.core.", "")
                .replace("            ", " ")
                .replace("\n", "")
        )
    }
}

data class Readme(val templateSets: List<TemplateSet>) : ViewModel

fun File.writeLiveTemplates() {

    val standard = TemplateSet(
        "Standard",
        listOf(
            Template("fil", "http4k Filter", """org.http4k.core.Filter { next -${"&"}gt;${"&"}#10;            {${"&"}#10;                ${"$"}content${"$"}${"&"}#10;                next(it)${"&"}#10;            }${"&"}#10;        }""", listOf("content")),
            Template("hh", "http4k Handler", """{ req: org.http4k.core.Request -&gt; org.http4k.core.Response(org.http4k.core.Status.OK) }""", listOf("content"))
        )
    )

    val methods = TemplateSet(
        "Request",
        Method.values().map {
            Template(
                it.name.toLowerCase(), "http4k ${it.name} Request",
                """org.http4k.core.Request(org.http4k.core.Method.${it.name}, &quot;${"$"}path${"$"}&quot;)""",
                listOf("path")
            )
        }
    )

    fun Status.statusTemplate(): Template = Template(code.toString(), "http4k $code Response", "org.http4k.core.Response(org.http4k.core.Status.Companion.${description.toUpperCase().replace(' ', '_')})")

    val statii = TemplateSet(
        "Response",
        listOf(
            Status.CONTINUE,
            Status.SWITCHING_PROTOCOLS,
            Status.OK,
            Status.CREATED,
            Status.ACCEPTED,
            Status.NON_AUTHORITATIVE_INFORMATION,
            Status.NO_CONTENT,
            Status.RESET_CONTENT,
            Status.PARTIAL_CONTENT,
            Status.MULTIPLE_CHOICES,
            Status.MOVED_PERMANENTLY,
            Status.FOUND,
            Status.SEE_OTHER,
            Status.NOT_MODIFIED,
            Status.USE_PROXY,
            Status.TEMPORARY_REDIRECT,
            Status.PERMANENT_REDIRECT,
            Status.BAD_REQUEST,
            Status.UNSATISFIABLE_PARAMETERS,
            Status.UNAUTHORIZED,
            Status.PAYMENT_REQUIRED,
            Status.FORBIDDEN,
            Status.NOT_FOUND,
            Status.METHOD_NOT_ALLOWED,
            Status.NOT_ACCEPTABLE,
            Status.PROXY_AUTHENTICATION_REQUIRED,
            Status.REQUEST_TIMEOUT,
            Status.CONFLICT,
            Status.GONE,
            Status.LENGTH_REQUIRED,
            Status.PRECONDITION_FAILED,
            Status.REQUEST_ENTITY_TOO_LARGE,
            Status.REQUEST_URI_TOO_LONG,
            Status.UNSUPPORTED_MEDIA_TYPE,
            Status.REQUESTED_RANGE_NOT_SATISFIABLE,
            Status.EXPECTATION_FAILED,
            Status.I_M_A_TEAPOT,
            Status.UNPROCESSABLE_ENTITY,
            Status.UPGRADE_REQUIRED,
            Status.TOO_MANY_REQUESTS,
            Status.INTERNAL_SERVER_ERROR,
            Status.NOT_IMPLEMENTED,
            Status.BAD_GATEWAY,
            Status.SERVICE_UNAVAILABLE,
            Status.CONNECTION_REFUSED,
            Status.UNKNOWN_HOST,
            Status.GATEWAY_TIMEOUT,
            Status.CLIENT_TIMEOUT,
            Status.HTTP_VERSION_NOT_SUPPORTED
        )
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
