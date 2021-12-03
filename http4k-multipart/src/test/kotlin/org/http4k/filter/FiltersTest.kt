package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType
import org.http4k.core.Method.POST
import org.http4k.core.MultipartFormBody
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.hamkrest.hasBody
import org.http4k.lens.Header
import org.http4k.lens.MultipartFormFile
import org.junit.jupiter.api.Test

class FiltersTest {

    @Test
    fun `process files filter and convert form from multipart webform`() {
        val form = MultipartFormBody("bob") + ("field" to "bar") +
            ("file" to MultipartFormFile("foo.txt", ContentType.TEXT_PLAIN, "content".byteInputStream())) +
            ("field" to "bar")

        val req = Request(POST, "")
            .with(Header.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(form.boundary))
            .body(form)

        val files = mutableListOf<String>()
        val service = ServerFilters.ProcessFiles {
            files.add(it.file.filename)
            it.file.filename
        }.then { r: Request ->
            Response(OK).body(r.body)
        }

        val response = service(req)

        assertThat(files, equalTo(listOf("foo.txt")))
        assertThat(response, hasBody("field=bar&file=foo.txt&field=bar"))
    }
}
