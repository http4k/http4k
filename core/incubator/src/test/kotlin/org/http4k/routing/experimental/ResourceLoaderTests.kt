package org.http4k.routing.experimental

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri.Companion.of
import org.http4k.hamkrest.hasStatus
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class DirectoryResourceLoaderTest : ResourceLoaderContract(ResourceLoaders.Directory("./src/test/resources")) {

    @Test
    fun `does not list directory`() {
        checkContents("org/http4k/routing", null, TEXT_HTML)
    }

    @Test
    fun `does not serve sibling directory sharing the base prefix`() {
        val temp = Files.createTempDirectory("resourceLoader").toFile()
        val base = File(temp, "base").apply { mkdirs() }
        File(temp, "baseEvil").apply { mkdirs() }.also { File(it, "secret.txt").writeText("secret") }

        val loader = ResourceLoaders.Directory(base.path)
        val request = Request(GET, of("../baseEvil/secret.txt"))
        assertThat(loader.match(request)(request), hasStatus(NOT_FOUND))
    }
}

class ListingDirectoryResourceLoaderTest :
    ResourceLoaderContract(ResourceLoaders.ListingDirectory("./src/test/resources")) {

    @Test
    fun `lists directory`() {
        @Language("HTML") val expected = """
        <html>
        <body>
        <h1>dir</h1>
        <ol>
        <li><a href="/dir/file.html">file.html</a></li>
        <li><a href="/dir/subdir">subdir</a></li>
        </ol>
        </body>
        </html>""".trimIndent()
        checkContents("/dir/", expected, TEXT_HTML)

        @Language("HTML") val expected2 = """
        <html>
        <body>
        <h1>subdir</h1>
        <ol>
        <li><a href="/dir/subdir/file.html">file.html</a></li>
        </ol>
        </body>
        </html>""".trimIndent()
        checkContents("/dir/subdir/", expected2, TEXT_HTML)
    }
}
