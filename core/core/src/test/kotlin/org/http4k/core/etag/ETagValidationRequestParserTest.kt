package org.http4k.core.etag

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.etag.ETagValidationRequestParser.Companion.parse
import org.http4k.core.etag.FieldValue.ETags
import org.junit.jupiter.api.Test

class ETagValidationRequestParserTest {

    @Test
    fun `strong etag with empty quotes`() = runBlocking {
        assertThat(parse("\"\""), equalTo(ETags()))
    }

    @Test
    fun `weak etag with empty quotes`() = runBlocking {
        assertThat(parse("W/\"\""), equalTo(ETags()))
    }

    @Test
    fun `no etags`() = runBlocking {
        assertThat(parse("something"), equalTo(ETags()))
    }

    @Test
    fun `strong etag`() = runBlocking {
        assertThat(parse("\"something\""), equalTo(ETags(ETag("something"))))
    }

    @Test
    fun `ignore content after matching quotes`() =
        assertThat(parse("\"something\"hey,a\""), equalTo(ETags(ETag("something"))))

    @Test
    fun `ignore content before matching quotes`() = runBlocking {
        assertThat(parse("something\"hey,a\""), equalTo(ETags()))
    }

    @Test
    fun `weak etag`() = runBlocking {
        assertThat(parse("W/\"hey\""), equalTo(ETags(ETag("hey", weak = true))))
    }

    @Test
    fun `weak etag followed by a comma`() = runBlocking {
        assertThat(parse("W/\"hey\","), equalTo(ETags(ETag("hey", weak = true))))
    }

    @Test
    fun `weak etag followed by a space and a comma`() = runBlocking {
        assertThat(parse("W/\"hey\" ,"), equalTo(ETags(ETag("hey", weak = true))))
    }

    @Test
    fun `weak etag preceded by spaces followed by a space and a comma`() = runBlocking {
        assertThat(parse("\t   W/\"hey\" ,"), equalTo(ETags(ETag("hey", weak = true))))
    }

    @Test
    fun `two weak etags comma and space separated`() = runBlocking {
        assertThat(parse("W/\"hey\", W/\"mate\""), equalTo(ETags(ETag("hey", weak = true), ETag("mate", weak = true))))
    }

    @Test
    fun `two weak etags with no separator extract just the first one`() = runBlocking {
        assertThat(parse("W/\"hey\" W/\"mate\""), equalTo(ETags(ETag("hey", weak = true))))
    }

    @Test
    fun `two strong etags comma separated`() = runBlocking {
        assertThat(parse("\"hey\", \"mate\""), equalTo(ETags(ETag("hey"), ETag("mate"))))
    }

    @Test
    fun `strong etag followed by few commas and quotes`() = runBlocking {
        assertThat(parse("\"hey\",\",\",\""), equalTo(ETags(ETag("hey"))))
    }

    @Test
    fun `strong etag followed by comma quote letter comma`() = runBlocking {
        assertThat(parse("\"hey\",\"y,\" , \""), equalTo(ETags(ETag("hey"), ETag("y,"))))
    }

    @Test
    fun `mix of weak and strong etags with invalid tag in between`() = runBlocking {
        val fieldValue = parse("""W/"hey",${"\"\"\""}b", ", ${"\t"}a ,text   W/"kk"  W/"text"  W/" sample""")
        assertThat(fieldValue, equalTo(ETags(ETag("hey", weak = true), ETag(", \ta ,text   W/"))))
    }

    @Test
    fun `a mix of well formed weak and strong etags`() = runBlocking {
        val fieldValue = parse("""
            W/"hello", "hey", "this", W/"is", "cool"
        """.trimIndent())

        assertThat(fieldValue, equalTo(ETags(
            ETag("hello", weak = true),
            ETag("hey"),
            ETag("this"),
            ETag("is", weak = true),
            ETag("cool")
        )))
    }
}
