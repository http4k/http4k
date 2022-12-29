package org.http4k.servirtium

import org.http4k.base64DecodedArray
import org.http4k.client.JavaHttpClient
import org.http4k.core.Body
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.BasicAuth
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.filter.HandleRemoteRequestFailed
import org.http4k.format.Moshi.auto
import org.http4k.lens.Query
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Read a file from a repository using the GitHub API.
 */
class GitHub @JvmOverloads constructor(private val owner: String,
                                       private val repo: String,
                                       credentials: Credentials,
                                       private val basePath: Path = Paths.get(""),
                                       private val reference: String? = null,
                                       http: HttpHandler = SetHostFrom(Uri.of("https://api.github.com")).then(JavaHttpClient())
) : StorageProvider {

    private val authed = BasicAuth(credentials)
        .then(ClientFilters.HandleRemoteRequestFailed())
        .then(http)

    override fun invoke(name: String): InteractionStorage = object : InteractionStorage {
        override fun get() = Body.auto<GithubFile>().toLens()(
            authed(Request(GET, "/repos/$owner/$repo/contents/$basePath/${name.replace(" ", "%20")}.md")
                .with(Query.optional("ref") of reference)
            )).decoded

        override fun clean() = throw UnsupportedOperationException("cannot clean a github file!")

        override fun accept(t: ByteArray) = throw UnsupportedOperationException("cannot upload to a github file")
    }
}

data class GithubFile(val content: String) {
    val decoded: ByteArray by lazy { content.replace("\n", "").base64DecodedArray() }
}
