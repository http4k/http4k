package org.http4k.server

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method

class ApacheServerTest : ServerContract(
    ::ApacheServer,
    ClientForServerTesting(),
    Method.entries.filter { it != Method.PURGE }.toTypedArray()
) {

    override fun requestScheme(): Matcher<String?> = equalTo("http")
}

