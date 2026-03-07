package org.http4k.wiretap

import org.http4k.template.DatastarElementRenderer
import org.http4k.testing.ApprovalTest
import org.http4k.wiretap.util.Templates
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
interface HttpWiretapFunctionContract {
    val function: WiretapFunction
    fun httpClient() = function.http(DatastarElementRenderer(Templates()), Templates())
}
