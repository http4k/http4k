package org.http4k.servirtium

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
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.filter.HandleRemoteRequestFailed
import org.http4k.format.Jackson.auto
import org.http4k.lens.Query
import java.util.Base64

/**
 * Read a file from a repository using the GitHub API.
 */
fun InteractionStorage.Companion.Github(
    owner: String,
    repo: String,
    reference: String?,
    credentials: Credentials,
    baseApiUri: Uri = Uri.of("https://api.github.com"),
    http: HttpHandler = SetBaseUriFrom(baseApiUri).then(JavaHttpClient())
) = object : StorageProvider {

    private val authed = BasicAuth(credentials)
        .then(ClientFilters.HandleRemoteRequestFailed())
        .then(http)

    override fun invoke(name: String): InteractionStorage = object : InteractionStorage {
        override fun get() = Body.auto<GithubFile>().toLens()(
            authed(Request(GET, "/repos/$owner/$repo/contents/$name")
                .with(Query.optional("ref") of reference)
            )).decoded

        override fun clean() = throw UnsupportedOperationException("cannot clean a github file!")

        override fun accept(t: ByteArray) = throw UnsupportedOperationException("cannot upload to a github file")
    }
}

data class GithubFile(val content: String) {
    val decoded by lazy { Base64.getDecoder().decode(content) }
}
