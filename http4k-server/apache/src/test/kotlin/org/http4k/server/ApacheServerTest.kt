package org.http4k.server

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.Method

class ApacheServerTest : ServerContract({ port -> ApacheServer(port, canonicalHostname = "localhost") },
    ApacheClient(),
    Method.values().filter { it != Method.PURGE }.toTypedArray()) {

    override fun requestScheme(): Matcher<String?> = equalTo("http")
}

class ApacheServerStopTest : ServerStopContract(
    { stopMode -> ApacheServer(0, canonicalHostname = "0.0.0.0", stopMode=stopMode) },
    ApacheClient(),
    {
        enableImmediateStop()
        enableGracefulStop()
    }
)
