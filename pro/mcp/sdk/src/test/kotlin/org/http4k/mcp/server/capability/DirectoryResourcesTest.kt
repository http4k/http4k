package org.http4k.mcp.server.capability

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.fs4k.dir
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.mcp.Client.Companion.NoOp
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.model.ResourceUriTemplate
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.server.capability.RecursionMode.Flat
import org.http4k.mcp.server.capability.RecursionMode.Recursive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class DirectoryResourcesTest {

    val path = "build/tmp_test"

    init {
        dir(path) {
            text("plainfile.txt") {
                content = "hello"
            }
            binary("binary.png") {
                content = "goodbye".byteInputStream()
            }
            dir("directory") {
                text("file2.html") {
                    content = "<html/>"
                }
            }
        }
    }

    @Test
    fun `can list files non-recursive`() {
        assertThat(
            DirectoryResources(File(path), Flat).listResources(McpResource.List.Request(), NoOp, Request(GET, "")),
            equalTo(
                McpResource.List.Response(
                    listOf(
                        McpResource(
                            uri = Uri.of("file://plainfile.txt"),
                            name = ResourceName.of("plainfile.txt"),
                            mimeType = MimeType.of("text/plain")
                        ),
                        McpResource(
                            uri = Uri.of("file://binary.png"),
                            name = ResourceName.of("binary.png"),
                            mimeType = MimeType.of("image/png")
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `can list files recursive`() {
        assertThat(
            DirectoryResources(File(path), Recursive).listResources(
                McpResource.List.Request(),
                NoOp,
                Request(GET, "")
            ),
            equalTo(
                McpResource.List.Response(
                    listOf(
                        McpResource(
                            uri = Uri.of("file://plainfile.txt"),
                            name = ResourceName.of("plainfile.txt"),
                            mimeType = MimeType.of("text/plain")
                        ),
                        McpResource(
                            uri = Uri.of("file://directory/file2.html"),
                            name = ResourceName.of("file2.html"),
                            mimeType = MimeType.of("text/html")
                        ),
                        McpResource(
                            uri = Uri.of("file://binary.png"),
                            name = ResourceName.of("binary.png"),
                            mimeType = MimeType.of("image/png")
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `can get templates non-recursive`() {
        assertThat(
            DirectoryResources(File(path), Flat).listTemplates(
                McpResource.ListTemplates.Request(),
                NoOp,
                Request(GET, "")
            ),
            equalTo(
                McpResource.ListTemplates.Response(
                    listOf(
                        McpResource(
                            uriTemplate = ResourceUriTemplate.of("file://{filename}"),
                            name = ResourceName.of("tmp_test"),
                            description = "Files in tmp_test, recursively: Flat"
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `can get templates recursive`() {
        assertThat(
            DirectoryResources(File(path), Recursive).listTemplates(
                McpResource.ListTemplates.Request(),
                NoOp,
                Request(GET, "")
            ),
            equalTo(
                McpResource.ListTemplates.Response(
                    listOf(
                        McpResource(
                            uriTemplate = ResourceUriTemplate.of("file://{+path}"),
                            name = ResourceName.of("tmp_test"),
                            description = "Files in tmp_test, recursively: Recursive"
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `can read text non-recursive`() {
        assertThat(
            DirectoryResources(File(path), Flat).read(
                McpResource.Read.Request(Uri.of("file://plainfile.txt")),
                NoOp,
                Request(GET, "")
            ),
            equalTo(
                McpResource.Read.Response(
                    listOf(
                        Resource.Content.Text(
                            "hello",
                            Uri.of("file://plainfile.txt"),
                            MimeType.of("text/plain")
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `can read binary non-recursive`() {
        assertThat(
            DirectoryResources(File(path), Flat).read(
                McpResource.Read.Request(Uri.of("file://binary.png")),
                NoOp,
                Request(GET, "")
            ),
            equalTo(
                McpResource.Read.Response(
                    listOf(
                        Resource.Content.Blob(
                            Base64Blob.of("Z29vZGJ5ZQ=="),
                            Uri.of("file://binary.png"),
                            MimeType.of("image/png")
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `can read recursive text`() {
        assertThat(
            DirectoryResources(File(path), Recursive).read(
                McpResource.Read.Request(Uri.of("file://directory/file2.html")),
                NoOp,
                Request(GET, "")
            ),
            equalTo(
                McpResource.Read.Response(
                    listOf(
                        Resource.Content.Text(
                            "<html/>",
                            Uri.of("file://directory/file2.html"),
                            MimeType.of(TEXT_HTML)
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `cannot read recursive when not`() {
        assertThrows<McpException> {
            DirectoryResources(File(path), Flat).read(
                McpResource.Read.Request(Uri.of("file://directory/file2.html")),
                NoOp,
                Request(GET, "")
            )
        }
    }
}
