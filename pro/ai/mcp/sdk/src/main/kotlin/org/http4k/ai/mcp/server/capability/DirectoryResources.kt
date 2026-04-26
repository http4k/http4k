/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.ResourceUriTemplate
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.server.capability.RecursionMode.Flat
import org.http4k.ai.mcp.server.capability.RecursionMode.Recursive
import org.http4k.ai.mcp.server.protocol.Resources
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
        req: McpResource.List.Request.Params,
        client: Client,
        http: Request
    ) = McpResource.List.Response.Result(
        when (recursive) {
            Flat -> dir.listFiles()?.filter { it.isFile }?.map { it.toResource() }?.asSequence() ?: emptySequence()
            Recursive -> dir.walkTopDown().filter { it.isFile }.map { it.toResource() }
        }.toList()
    )

    override fun listTemplates(
        req: McpResource.ListTemplates.Request.Params,
        client: Client,
        http: Request
    ) = McpResource.ListTemplates.Response.Result(
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

    override fun invoke(resourceRequest: ResourceRequest) = ResourceResponse.Ok(load(resourceRequest.uri))

    override fun read(req: McpResource.Read.Request.Params, client: Client, http: Request) =
        McpResource.Read.Response.Result(load(req.uri))

    private fun load(uri: Uri): List<Resource.Content> {
        val path = uri.toString().substringAfter("file://")
        when {
            path.contains(separatorChar) && recursive == Flat -> throw McpException(InvalidParams)

            else -> {
                val file = File(dir, path)
                val contentType = mimeTypes.forFile(file.name)

                return when {
                    file.isFile && file.exists() ->
                        listOf(
                            when {
                                isText(contentType.withNoDirectives()) -> Resource.Content.Text(
                                    file.readText(),
                                    uri,
                                    MimeType.of(contentType)
                                )

                                else -> Resource.Content.Blob(
                                    Base64Blob.encode(file.readBytes()), uri, MimeType.of(contentType)
                                )
                            }
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

    override var items: Iterable<ResourceCapability>
        get() = throw UnsupportedOperationException()
        set(value) = throw UnsupportedOperationException()

    override fun iterator() = items.iterator()

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
