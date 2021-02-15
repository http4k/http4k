package org.http4k.routing.experimental

import org.http4k.core.ContentType
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class DirectoryResourceLoaderTest : ResourceLoaderContract(ResourceLoaders.Directory("./src/test/resources")) {

    @Test
    fun `does not list directory`() {
        checkContents("org/http4k/routing", null, ContentType.TEXT_HTML)
    }
}

class ListingDirectoryResourceLoaderTest : ResourceLoaderContract(ResourceLoaders.ListingDirectory("./src/test/resources")) {

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
        checkContents("/dir/", expected, ContentType.TEXT_HTML)

        @Language("HTML") val expected2 = """
        <html>
        <body>
        <h1>subdir</h1>
        <ol>
        <li><a href="/dir/subdir/file.html">file.html</a></li>
        </ol>
        </body>
        </html>""".trimIndent()
        checkContents("/dir/subdir/", expected2, ContentType.TEXT_HTML)
    }
}
