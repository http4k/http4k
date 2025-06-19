package org.http4k.connect.amazon.instancemetadata.endpoints

import org.http4k.core.Method.PUT
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind

fun getToken() = "/latest/api/token" bind PUT to {
    Response(OK).body("secretToken")
}

