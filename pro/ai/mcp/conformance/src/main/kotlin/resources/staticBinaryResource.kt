package resources

import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.core.Uri
import org.http4k.routing.bind
import tools.imageContent

fun staticBinaryResource() =
    Resource.Static(Uri.of("test://static-binary"), ResourceName.of("static-binary"), null, imageContent.mimeType) bind {
        ResourceResponse(
            listOf(
                Resource.Content.Blob(
                    imageContent.data,
                    Uri.of("test://static-binary"),
                    imageContent.mimeType
                )
            )
        )
    }
