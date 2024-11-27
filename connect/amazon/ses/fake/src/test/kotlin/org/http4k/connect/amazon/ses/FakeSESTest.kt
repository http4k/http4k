package org.http4k.connect.amazon.ses

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.fakeAwsEnvironment
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage

class FakeSESTest : SESContract, FakeAwsContract {

    override val http = FakeSES(messagesBySender)

    override fun assertEmailSent() {
        assertThat(messagesBySender[from.value]?.size, equalTo(1))
    }

    companion object {
        val messagesBySender = Storage.InMemory<List<EmailMessage>>()
    }
}
