package org.http4k.connect.amazon.lambda

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.functions
import org.http4k.serverless.FnHandler
import org.http4k.serverless.FnLoader
import org.http4k.serverless.InvocationFnLoader
import java.io.InputStream

val functions = functions(
    "event" bind FnLoader {
        FnHandler { e: ScheduledEvent, _ ->
            e
        }
    },
    "http" bind InvocationFnLoader {
        Response(OK).body(it.bodyString() + it.bodyString())
    },
    "stream" bind { _: Map<String, String> ->
        FnHandler { e: InputStream, _ ->
            e
        }
    }
)
