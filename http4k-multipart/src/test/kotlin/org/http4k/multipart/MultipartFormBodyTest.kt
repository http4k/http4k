package org.http4k.multipart

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.ContentType
import org.junit.Test

class MultipartFormBodyTest {

    @Test
    fun `roundtrip`() {
        val form = MultipartFormBody(listOf(
            MultipartEntity.Form("field", "bar"),
            MultipartEntity.File("file", "foo.txt", ContentType.TEXT_PLAIN, "content".byteInputStream())
        ), "bob")

        form
            .let { MultipartFormBody.from(it, form.boundary) }
            .shouldMatch(equalTo(MultipartFormBody(listOf(
                MultipartEntity.Form("field", "bar"),
                MultipartEntity.File("file", "foo.txt", ContentType.TEXT_PLAIN, "content".byteInputStream())
            ), "bob")
            ))
    }
}