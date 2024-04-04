package org.http4k.core.etag

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ETagValidationRequestParserTest {

    @Test
    fun `strong etag with empty quotes`() {
        assertThat(ETagValidationRequestParser.parse("\"\""), equalTo(FieldValue.ETags()))
    }

    @Test
    fun `weak etag with empty quotes`() {
        assertThat(ETagValidationRequestParser.parse("W/\"\""), equalTo(FieldValue.ETags()))
    }

    @Test
    fun `no etags`() {
        assertThat(ETagValidationRequestParser.parse("something"), equalTo(FieldValue.ETags()))
    }

    @Test
    fun `strong etag`() {
        assertThat(ETagValidationRequestParser.parse("\"something\""), equalTo(FieldValue.ETags(ETag("something"))))
    }

    @Test
    fun `ignore content after matching quotes`() =
        assertThat(
            ETagValidationRequestParser.parse("\"something\"hey,a\""),
            equalTo(FieldValue.ETags(ETag("something")))
        )

    @Test
    fun `ignore content before matching quotes`() {
        assertThat(ETagValidationRequestParser.parse("something\"hey,a\""), equalTo(FieldValue.ETags()))
    }

    @Test
    fun `weak etag`() {
        assertThat(ETagValidationRequestParser.parse("W/\"hey\""), equalTo(FieldValue.ETags(ETag("hey", weak = true))))
    }

    @Test
    fun `weak etag followed by a comma`() {
        assertThat(ETagValidationRequestParser.parse("W/\"hey\","), equalTo(FieldValue.ETags(ETag("hey", weak = true))))
    }

    @Test
    fun `weak etag followed by a space and a comma`() {
        assertThat(
            ETagValidationRequestParser.parse("W/\"hey\" ,"),
            equalTo(FieldValue.ETags(ETag("hey", weak = true)))
        )
    }

    @Test
    fun `weak etag preceded by spaces followed by a space and a comma`() {
        assertThat(
            ETagValidationRequestParser.parse("\t   W/\"hey\" ,"),
            equalTo(FieldValue.ETags(ETag("hey", weak = true)))
        )
    }

    @Test
    fun `two weak etags comma and space separated`() {
        assertThat(
            ETagValidationRequestParser.parse("W/\"hey\", W/\"mate\""),
            equalTo(FieldValue.ETags(ETag("hey", weak = true), ETag("mate", weak = true)))
        )
    }

    @Test
    fun `two weak etags with no separator extract just the first one`() {
        assertThat(
            ETagValidationRequestParser.parse("W/\"hey\" W/\"mate\""),
            equalTo(FieldValue.ETags(ETag("hey", weak = true)))
        )
    }

    @Test
    fun `two strong etags comma separated`() {
        assertThat(
            ETagValidationRequestParser.parse("\"hey\", \"mate\""),
            equalTo(FieldValue.ETags(ETag("hey"), ETag("mate")))
        )
    }

    @Test
    fun `strong etag followed by few commas and quotes`() {
        assertThat(ETagValidationRequestParser.parse("\"hey\",\",\",\""), equalTo(FieldValue.ETags(ETag("hey"))))
    }

    @Test
    fun `strong etag followed by comma quote letter comma`() {
        assertThat(
            ETagValidationRequestParser.parse("\"hey\",\"y,\" , \""),
            equalTo(FieldValue.ETags(ETag("hey"), ETag("y,")))
        )
    }

    @Test
    fun `mix of weak and strong etags with invalid tag in between`() {
        val fieldValue =
            ETagValidationRequestParser.parse("""W/"hey",${"\"\"\""}b", ", ${"\t"}a ,text   W/"kk"  W/"text"  W/" sample""")
        assertThat(fieldValue, equalTo(FieldValue.ETags(ETag("hey", weak = true), ETag(", \ta ,text   W/"))))
    }

    @Test
    fun `a mix of well formed weak and strong etags`() {
        val fieldValue = ETagValidationRequestParser.parse(
            """
            W/"hello", "hey", "this", W/"is", "cool"
        """.trimIndent()
        )

        assertThat(
            fieldValue, equalTo(
                FieldValue.ETags(
                    ETag("hello", weak = true),
                    ETag("hey"),
                    ETag("this"),
                    ETag("is", weak = true),
                    ETag("cool")
                )
            )
        )
    }
}
