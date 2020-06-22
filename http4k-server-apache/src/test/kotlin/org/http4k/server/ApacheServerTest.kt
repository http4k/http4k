package org.http4k.server

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters

class ApacheServerTest : ServerContract({ port -> ApacheServer(port, canonicalHostname = "0.0.0.0") },
    DebuggingFilters.PrintRequestAndResponse().then(ApacheClient()),
    Method.values().filter { it != Method.PURGE }.toTypedArray()) {

    override fun requestScheme(): Matcher<String?> = equalTo("http")
}
