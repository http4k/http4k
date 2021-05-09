package org.http4k.serverless.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import org.http4k.format.AutoMarshalling
import org.http4k.format.AwsLambdaMoshi
import org.http4k.serverless.FunctionHandler

/**
 * FunctionHandler for AWS Lambda ScheduledEvent
 */
@JvmName("bob")
fun <Out : Any> FunctionLoader(
    autoMarshalling: AutoMarshalling = AwsLambdaMoshi,
    fn: FunctionHandler<ScheduledEvent, Context, Out>):
        (Map<String, String>) -> FunctionHandler<Map<String, String>, Context, Out> = TODO()
//    AdaptingFunctionHandler(fn) {
//        @Suppress("UNCHECKED_CAST")
//        ScheduledEvent().apply {
//            id = it["id"]?.toString()
//            detailType = it["detail-type"]?.toString()
//            source = it["source"]?.toString()
//            account = it["account"]?.toString()
//            time = it["time"]?.toString()?.let(DateTime::parse)
//            region = it["region"]?.toString()
//            resources = it["resources"] as List<String>?
//            detail = it["detail"] as Map<String, Any>?
//        }
//    }
