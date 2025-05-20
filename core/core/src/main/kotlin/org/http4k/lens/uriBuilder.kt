package org.http4k.lens

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with

/**
 * Typesafe Uri builder. Activate it by injecting lenses (Query, Path, Header) into the Uri
 */
fun Uri.with(vararg inject: (Request) -> Request): Uri = Request(GET, this).with(*inject).uri
