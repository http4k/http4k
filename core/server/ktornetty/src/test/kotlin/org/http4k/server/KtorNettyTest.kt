package org.http4k.server

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Disabled
import java.util.Random

class KtorNettyTest : ServerContract(
    { _, stopMode -> KtorNetty(Random().nextInt(1000) + 7456, stopMode = stopMode) },
    ClientForServerTesting()
) {

    @Disabled
    override fun `ok when length already set`() = runBlocking {
    }

    override fun clientAddress(): Matcher<String?> = present()

    override fun requestScheme(): Matcher<String?> = equalTo("http")

}
