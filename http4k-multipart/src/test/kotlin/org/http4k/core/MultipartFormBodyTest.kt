package org.http4k.core

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.lens.Header
import org.junit.Test

class MultipartFormBodyTest {

    @Test
    fun `roundtrip`() {
        val form = MultipartFormBody(listOf(
            MultipartEntity.Form("field", "bar"),
            MultipartEntity.File("file", "foo.txt", ContentType.TEXT_PLAIN, "content".byteInputStream())
        ), "bob")

        val req = Request(Method.POST, "")
            .with(Header.Common.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(form.boundary))
            .body(form)

        MultipartFormBody.from(req) shouldMatch equalTo(
            MultipartFormBody(listOf(
                MultipartEntity.Form("field", "bar"),
                MultipartEntity.File("file", "foo.txt", ContentType.TEXT_PLAIN, "content".byteInputStream())
            ), "bob")
        )
    }
}