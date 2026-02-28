package org.http4k.wiretap.traffic

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.long
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.core.Method.DELETE
import org.http4k.lens.Path
import org.http4k.lens.long
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.View
import org.http4k.wiretap.domain.ViewId

fun DeleteView(remove: (ViewId) -> Unit, list: () -> List<View>) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/views/{id}" bind DELETE to { req ->
            val id = Path.long().of("id")(req)
            remove(id)
            elements.renderViewBar(list())
        }

    override fun mcp(): ToolCapability {
        val id = Tool.Arg.long().required("id", "ID of the view to delete")

        return Tool(
            "delete_view",
            "Delete a transaction filter view",
            id
        ) bind {
            remove(id(it))
            Ok(listOf(Text("View deleted")))
        }
    }
}
