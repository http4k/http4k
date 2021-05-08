package org.http4k.serverless.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import org.http4k.serverless.AdaptingFunctionHandler
import org.http4k.serverless.FunctionHandler
import org.joda.time.DateTime

/**
 * FunctionHandler for AWS Lambda ScheduledEvent
 */
operator fun <Out : Any> FunctionHandler.Companion.invoke(fn: (ScheduledEvent, Context) -> Out) =
    AdaptingFunctionHandler(fn) {
        @Suppress("UNCHECKED_CAST")
        ScheduledEvent().apply {
            id = it["id"]?.toString()
            detailType = it["detail-type"]?.toString()
            source = it["source"]?.toString()
            account = it["account"]?.toString()
            time = it["time"]?.toString()?.let(DateTime::parse)
            region = it["region"]?.toString()
            resources = it["resources"] as List<String>?
            detail = it["detail"] as Map<String, Any>?
        }
    }
