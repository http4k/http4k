package org.http4k.multipart

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.ContentType
import org.junit.Test

class MultipartFormEntityTest {

    @Test
    fun `roundtrip`() {
        val form = MultipartFormEntity(listOf(
            MultipartEntity.Form("field", "bar"),
            MultipartEntity.File("file", "foo.txt", ContentType.TEXT_PLAIN, "content".byteInputStream())
        ), "bob")

        form.toBody()
            .let { MultipartFormEntity.fromBody(it, form.boundary) }
            .shouldMatch(equalTo(MultipartFormEntity(listOf(
                MultipartEntity.Form("field", "bar"),
                MultipartEntity.File("file", "foo.txt", ContentType.TEXT_PLAIN, "content".byteInputStream())
            ), "bob")
            ))
    }
}