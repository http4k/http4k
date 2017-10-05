package org.http4k.multipart

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.ContentType
import org.junit.Test

class MultipartFormTest {

    @Test
    fun `roundtrip`() {
        val form = MultipartForm(listOf(
            Multipart.FormField("field", "bar"),
            Multipart.FormFile("file", "foo.txt", ContentType.TEXT_PLAIN, "content".byteInputStream())
        ), "bob")

        form.toBody()
            .let { MultipartForm.fromBody(it, form.boundary) }
            .shouldMatch(equalTo(MultipartForm(listOf(
                Multipart.FormField("field", "bar"),
                Multipart.FormFile("file", "foo.txt", ContentType.TEXT_PLAIN, "content".byteInputStream())
            ), "bob")
            ))
    }
}