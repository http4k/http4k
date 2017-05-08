package org.http4k.http

import org.http4k.http.core.HttpHandler
import org.http4k.http.core.Response

fun Response.toHttpHandler(): HttpHandler = { this }

