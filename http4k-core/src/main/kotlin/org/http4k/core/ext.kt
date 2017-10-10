package org.http4k.core

private fun Request.ensureValidMethod(): Request =
    if (method == Method.GET || method == Method.HEAD) this else method(Method.GET)

private fun Request.newLocation(location: String): Uri {
    val locationUri = Uri.of(location)
    return if (locationUri.host.isBlank()) {
        locationUri.authority(uri.authority).scheme(uri.scheme)
    } else locationUri
}

fun Request.toNewLocation(location: String) = ensureValidMethod().uri(newLocation(location))

fun Response.location() = header("location")?.removeCharset().orEmpty()

fun Response.assureBodyIsConsumed() = body.stream.close()

fun Response.isRedirection(): Boolean {
    return status.redirection && header("location")?.let(String::isNotBlank) == true
}

private fun String.removeCharset(): String = this.replace(";\\s*charset=.*$".toRegex(), "")
