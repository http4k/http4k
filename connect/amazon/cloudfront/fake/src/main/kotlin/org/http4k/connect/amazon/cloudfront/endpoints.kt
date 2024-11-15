package org.http4k.connect.amazon.cloudfront

import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.routing.bind
import org.http4k.template.PebbleTemplates
import org.http4k.template.ViewModel
import org.http4k.template.viewModel
import java.time.Clock
import java.time.Instant


fun CreateInvalidation(clock: Clock) =
    "/2020-05-31/distribution/{distributionId}/invalidation" bind POST to {
        Response(OK).with(lens of Invalidation(it.bodyString(), clock.instant()))
    }

data class Invalidation(private val request: String, val time: Instant) : ViewModel {
    val batch = request.trim().drop("""<?xml version="1.0" encoding="UTF-8"?>""".length)
}

private val lens by lazy {
    Body.viewModel(PebbleTemplates().CachingClasspath(), APPLICATION_XML).toLens()
}
