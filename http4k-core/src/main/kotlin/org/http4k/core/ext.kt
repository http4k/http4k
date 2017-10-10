package org.http4k.core

fun Request.toNewLocation(location: String) = ensureValidMethodForRedirect().uri(newLocation(location))

fun Response.location() = header("location")?.replace(";\\s*charset=.*$".toRegex(), "").orEmpty()

fun Response.assureBodyIsConsumed() = body.stream.close()

fun Response.isRedirection(): Boolean {
    return status.redirection && header("location")?.let(String::isNotBlank) == true
}

private fun Request.ensureValidMethodForRedirect(): Request =
    if (method == Method.GET || method == Method.HEAD) this else method(Method.GET)

private fun Request.newLocation(location: String): Uri {
    val locationUri = Uri.of(location)
    return if (locationUri.host.isBlank()) {
        locationUri.authority(uri.authority).scheme(uri.scheme)
    } else locationUri
}

