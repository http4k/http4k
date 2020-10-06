package org.http4k.lens

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.ContentType
import org.junit.jupiter.api.Test

class MultipartFormFileTest {
    private fun file1() = MultipartFormFile("world", ContentType.TEXT_HTML, "world".byteInputStream())
    private fun file2() = MultipartFormFile("world2", ContentType.TEXT_PLAIN, "world2".byteInputStream())

    private fun form() = MultipartForm()
        .plus("hello" to file1())
        .plus("hello" to file2())

    @Test
    fun `value present`() {
        assertThat(MultipartFormFile.optional("hello")(form()), equalTo(file1()))
        assertThat(MultipartFormFile.required("hello")(form()), equalTo(file1()))
        assertThat(MultipartFormFile.map { it.filename }.required("hello")(form()), equalTo(file1().filename))
        assertThat(MultipartFormFile.map { it.filename }.optional("hello")(form()), equalTo(file1().filename))

        assertThat(MultipartFormFile.multi.required("hello")(form()), equalTo(listOf(file1(), file2())))
        assertThat(MultipartFormFile.multi.optional("hello")(form()), equalTo(listOf(file1(), file2())))
    }

    @Test
    fun `value missing`() {
        assertThat(MultipartFormFile.optional("world")(form()), absent())
        val requiredFormFile = MultipartFormFile.required("world")
        assertThat({ requiredFormFile(form()) }, throws(lensFailureWith<MultipartForm>(Missing(requiredFormFile.meta), overallType = Failure.Type.Missing)))

        assertThat(MultipartFormFile.multi.optional("world")(form()), absent())
        val optionalMultiFormFile = MultipartFormFile.multi.required("world")
        assertThat({ optionalMultiFormFile(form()) }, throws(lensFailureWith<MultipartForm>(Missing(optionalMultiFormFile.meta), overallType = Failure.Type.Missing)))
    }

    @Test
    fun `value replaced`() {
        val single = MultipartFormFile.required("world")
        assertThat(single(file2(), single(file1(), MultipartForm())), equalTo(MultipartForm() + ("world" to file2())))

        val multi = MultipartFormFile.multi.required("world")
        assertThat(
            multi(listOf(file2(), file2()), multi(listOf(file1(), file1()), MultipartForm())),
            equalTo(MultipartForm() + ("world" to file2()) + ("world" to file2()))
        )
    }

    @Test
    fun `invalid value`() {
        val requiredFormFile = MultipartFormFile.map(Any::toString).map(String::toInt).required("hello")
        assertThat({ requiredFormFile(form()) }, throws(lensFailureWith<MultipartForm>(Invalid(requiredFormFile.meta), overallType = Failure.Type.Invalid)))

        val optionalFormFile = MultipartFormFile.map(Any::toString).map(String::toInt).optional("hello")
        assertThat({ optionalFormFile(form()) }, throws(lensFailureWith<MultipartForm>(Invalid(optionalFormFile.meta), overallType = Failure.Type.Invalid)))

        val requiredMultiFormFile = MultipartFormFile.map(Any::toString).map(String::toInt).multi.required("hello")
        assertThat({ requiredMultiFormFile(form()) }, throws(lensFailureWith<MultipartForm>(Invalid(requiredMultiFormFile.meta), overallType = Failure.Type.Invalid)))

        val optionalMultiFormFile = MultipartFormFile.map(Any::toString).map(String::toInt).multi.optional("hello")
        assertThat({ optionalMultiFormFile(form()) }, throws(lensFailureWith<MultipartForm>(Invalid(optionalMultiFormFile.meta), overallType = Failure.Type.Invalid)))
    }

    @Test
    fun `sets value on form`() {
        val formFile = MultipartFormFile.required("bob")
        val withFormFile = formFile(file1(), form())
        assertThat(formFile(withFormFile), equalTo(file1()))
    }

    @Test
    fun `toString is ok`() {
        assertThat(MultipartFormFile.required("hello").toString(), equalTo("Required form 'hello'"))
        assertThat(MultipartFormFile.optional("hello").toString(), equalTo("Optional form 'hello'"))
        assertThat(MultipartFormFile.multi.required("hello").toString(), equalTo("Required form 'hello'"))
        assertThat(MultipartFormFile.multi.optional("hello").toString(), equalTo("Optional form 'hello'"))
    }
}
