package org.http4k.ai.a2a.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Uri
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class PartTest {

    @Test
    fun `TextPart roundtrips correctly`(approver: Approver) {
        val part = Part.Text("Hello, world!")
        val json = A2AJson.asFormatString(part)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<Part>(json), equalTo(part))
    }

    @Test
    fun `FilePart with data roundtrips correctly`(approver: Approver) {
        val part = Part.File(
            data = Base64Blob.encode("file content"),
            mimeType = MimeType.of("text/plain"),
            name = "test.txt"
        )
        val json = A2AJson.asFormatString(part)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<Part>(json), equalTo(part))
    }

    @Test
    fun `FilePart with uri roundtrips correctly`(approver: Approver) {
        val part = Part.File(
            uri = Uri.of("https://example.com/file.txt"),
            mimeType = MimeType.of("text/plain"),
            name = "file.txt"
        )
        val json = A2AJson.asFormatString(part)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<Part>(json), equalTo(part))
    }

    @Test
    fun `DataPart roundtrips correctly`(approver: Approver) {
        val part = Part.Data(mapOf("key" to "value", "number" to 42.0))
        val json = A2AJson.asFormatString(part)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<Part>(json), equalTo(part))
    }
}
