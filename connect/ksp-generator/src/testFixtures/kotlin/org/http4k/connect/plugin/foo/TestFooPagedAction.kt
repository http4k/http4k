package org.http4k.connect.plugin.foo

import org.http4k.connect.AutomarshalledPagedAction
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.kClass
import org.http4k.connect.plugin.TestPaged
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.format.Moshi

@Http4kConnectAction
data class TestFooPagedAction(val input: String, val input2: TestEnum) :
    AutomarshalledPagedAction<Uri, String, TestPaged, TestFooPagedAction>(
        { _, _ -> TestPaged(null) }, Moshi, kClass()
    ), FooAction<TestPaged> {

    override fun toRequest() = Request(GET, "")
    override fun invoke(target: Response) = null

    override fun next(token: Uri) = TestFooPagedAction(token.toString(), input2)
}
