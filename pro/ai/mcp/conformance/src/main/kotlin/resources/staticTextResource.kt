package resources

import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import org.http4k.routing.bind

fun staticTextResource() =
    Resource.Static(Uri.of("test://static-text"), ResourceName.of("static-text"), null, MimeType.TEXT_PLAIN) bind {
        ResourceResponse(
            listOf(
                Resource.Content.Text(
                    "This is the content of the static text resource.",
                    Uri.of("test://static-text"),
                    MimeType.TEXT_PLAIN
                )
            )
        )
    }


