package org.http4k.connect.amazon.iotdataplane.action

import org.http4k.connect.amazon.iotdataplane.model.ShadowName
import org.http4k.connect.amazon.iotdataplane.model.ThingName
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri

/** An absent [shadowName] addresses the unnamed ("classic") shadow. */
internal fun shadowRequest(method: Method, thingName: ThingName, shadowName: ShadowName?): Request {
    val request = Request(method, Uri.of("").path("/things/${thingName.value}/shadow"))
    return shadowName?.let { request.query("name", it.value) } ?: request
}
