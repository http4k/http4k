package org.reekwest.http.filters

import org.reekwest.http.Credentials
import org.reekwest.http.base64Encode
import org.reekwest.http.core.Filter
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Uri

object ClientFilters {

    fun FollowRedirects(): Filter = Filter { next -> { makeRequest(next, it) } }

    private fun makeRequest(next: HttpHandler, request: Request, attempt: Int = 1): Response {
        val response = next(request)
        return if (response.isRedirection() && request.allowsRedirection()) {
            if (attempt == 10) throw IllegalStateException("Too many redirection")
            val location = response.header("location").orEmpty()
            makeRequest(next, request.copy(uri = request.newLocation(location)), attempt + 1)
        } else {
            response
        }
    }

    private fun Request.newLocation(location: String): Uri {
        val locationUri = Uri.uri(location)
        return if (locationUri.host.isBlank()) {
            locationUri.copy(uri.scheme, uri.authority, location)
        } else locationUri
    }

    private fun Response.isRedirection(): Boolean {
        return status.redirection && header("location")?.let(String::isNotBlank) ?: false
    }

    private fun Request.allowsRedirection(): Boolean = method != Method.POST && method != Method.PUT

    fun BasicAuth(provider: () -> Credentials): Filter = Filter {
        next ->
        { next(it.header("Authorization", "Basic ${provider().base64Encoded()}")) }
    }

    fun BasicAuth(user: String, password: String): Filter = BasicAuth(Credentials(user, password))
    fun BasicAuth(credentials: Credentials): Filter = BasicAuth({ credentials })

    private fun Credentials.base64Encoded(): String = "$user:$password".base64Encode()
}


