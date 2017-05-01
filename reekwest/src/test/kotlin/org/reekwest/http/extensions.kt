package org.reekwest.http

import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Response

fun Response.toHttpHandler(): HttpHandler = { this }

