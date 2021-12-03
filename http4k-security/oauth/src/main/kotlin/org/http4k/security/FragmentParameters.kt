package org.http4k.security

import org.http4k.core.MemoryRequest
import org.http4k.core.Parameters
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.findMultiple
import org.http4k.core.findSingle
import org.http4k.core.toParameters
import org.http4k.core.toUrlFormEncoded

fun Uri.removeFragmentParameter(name: String) = copy(fragment = fragment.toParameters().filterNot { it.first == name }.toUrlFormEncoded())

fun Uri.fragmentParameter(name: String, value: String?): Uri = copy(fragment = fragment.toParameters().plus(name to value).toUrlFormEncoded())

fun Uri.fragmentParameters(): Parameters = fragment.toParameters()

/**
 * Retrieves the first fragment parameter value with this name.
 */
fun Request.fragmentParameter(name: String): String? = uri.fragmentParameters().findSingle(name)

/**
 * (Copy &) Adds a query value with this name.
 */
fun Request.fragmentParameter(name: String, value: String?): Request = MemoryRequest(this.method, this.uri.fragmentParameter(name, value), this.headers, this.body, this.version)

/**
 * Retrieves all fragment parameters with this name.
 */
fun Request.fragmentParameters(name: String): List<String?> = uri.fragmentParameters().findMultiple(name)
