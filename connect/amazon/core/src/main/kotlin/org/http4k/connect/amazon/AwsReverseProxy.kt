package org.http4k.connect.amazon

import org.http4k.core.HttpHandler
import org.http4k.routing.reverseProxy

/**
 * Simple ReverseProxy for AWS services. Allows for simple traffic splitting based
 * on the Host header.
 */
fun AwsReverseProxy(vararg awsServices: Pair<AwsServiceCompanion, HttpHandler>) =
    reverseProxy(*awsServices.map { it.first.awsService.value to it.second }.toTypedArray())

