package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.lens.Header
import org.http4k.multipart.AlreadyClosedException
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference

class MultipartIteratorTest {

    @Test
    fun `can stream multiparts`() {
        val form = MultipartFormBody("bob") + ("field" to "bar") +
            ("file" to FormFile("foo.txt", ContentType.TEXT_PLAIN, "content".byteInputStream())) +
            ("field2" to "bar2")

        val req = Request(Method.POST, "")
            .with(Header.Common.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(form.boundary))
            .body(form)

        val fileStream = AtomicReference<InputStream>()

        val fields = req.multipartIterator().asSequence().fold(emptyList<MultipartEntity.Field>()) { memo, next ->
            when (next) {
                is MultipartEntity.File -> {
                    fileStream.set(next.file.content)
                    memo
                }
                is MultipartEntity.Field -> memo.plus(next)
            }
        }
        assertThat({ String(fileStream.get().readBytes()) }, throws<AlreadyClosedException>())

        assertThat(fields, equalTo(listOf(
            MultipartEntity.Field("field", "bar"),
            MultipartEntity.Field("field2", "bar2")
        )))
    }

}