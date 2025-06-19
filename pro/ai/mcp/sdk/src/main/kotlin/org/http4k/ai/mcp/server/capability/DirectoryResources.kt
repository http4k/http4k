package org.http4k.ai.mcp.server.capability

import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.ContentType.Companion.APPLICATION_YAML
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.MimeTypes
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.ResourceUriTemplate
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.server.capability.RecursionMode.Flat
import org.http4k.ai.mcp.server.capability.RecursionMode.Recursive
import org.http4k.ai.mcp.server.protocol.Resources
import java.io.File
import java.io.File.separatorChar

/**
 * Simple file-based resources.
 */
class DirectoryResources(
    private val dir: File,
    private val recursive: RecursionMode,
    private val mimeTypes: MimeTypes = MimeTypes(),
    private val isText: (ContentType) -> Boolean = DEFAULT_TEXT_TYPES,
) : Resources {
    override fun listResources(
        req: McpResource.List.Request,
        client: Client,
        http: Request
    ) = McpResource.List.Response(
        when (recursive) {
            Flat -> dir.listFiles()?.filter { it.isFile }?.map { it.toResource() }?.asSequence() ?: emptySequence()
            Recursive -> dir.walkTopDown().filter { it.isFile }.map { it.toResource() }
        }.toList()
    )

    override fun listTemplates(
        req: McpResource.ListTemplates.Request,
        client: Client,
        http: Request
    ) = McpResource.ListTemplates.Response(
        listOf(
            McpResource(
                ResourceUriTemplate.of(
                    when (recursive) {
                        Flat -> "file://{filename}"
                        Recursive -> "file://{+path}"
                    }
                ),
                ResourceName.of(dir.name),
                "Files in ${dir.name}, recursively: $recursive",
            )
        )
    )

    override fun read(req: McpResource.Read.Request, client: Client, http: Request): McpResource.Read.Response {
        val path = req.uri.toString().substringAfter("file://")
        when {
            path.contains(separatorChar) && recursive == Flat -> throw McpException(InvalidParams)

            else -> {
                val file = File(dir, path)
                val contentType = mimeTypes.forFile(file.name)

                return when {
                    file.isFile && file.exists() -> McpResource.Read.Response(
                        listOf(
                            when {
                                isText(contentType.withNoDirectives()) -> Resource.Content.Text(
                                    file.readText(),
                                    req.uri,
                                    MimeType.of(contentType)
                                )

                                else -> Resource.Content.Blob(
                                    Base64Blob.encode(file.readBytes()), req.uri, MimeType.of(contentType)
                                )
                            }
                        )
                    )

                    else -> throw McpException(InvalidParams)
                }
            }
        }
    }

    private fun File.toResource() = McpResource(
        uri = Uri.of("file://${relativeTo(dir)}"),
        name = ResourceName.of(name),
        mimeType = MimeType.of(mimeTypes.forFile(name)),
        size = null,
        annotations = null
    )

    companion object {
        val DEFAULT_TEXT_TYPES =
            { element: ContentType ->
                setOf(
                    APPLICATION_JSON,
                    APPLICATION_XML,
                    TEXT_HTML,
                    TEXT_PLAIN,
                    APPLICATION_YAML
                ).map { it.withNoDirectives() }
                    .contains(element)
            }
    }
}

enum class RecursionMode {
    Flat, Recursive
}
