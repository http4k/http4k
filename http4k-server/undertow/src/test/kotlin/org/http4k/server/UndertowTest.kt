package org.http4k.server

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient

class UndertowTest : ServerContract({ Undertow(it) }, ApacheClient()) {
    override fun requestScheme(): Matcher<String?> = equalTo("http")
}
