package org.http4k

import org.http4k.core.HttpHandler
import org.http4k.core.Response

fun Response.toHttpHandler() = HttpHandler { this }