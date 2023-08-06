package org.http4k.lens

import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OptionalFormParameterExtractionTests {

    private val intLens: BiDiLens<WebForm, Int?> = FormField.int().optional("anInt")
    private val bodyLens = Body.webForm(Validator.Strict, intLens).toLens()

    private val baseRequest = Request(POST, "/form-submit")
        .with(CONTENT_TYPE of APPLICATION_FORM_URLENCODED)

    @Test
    fun `int string should parse`() {
        val form = bodyLens(baseRequest.form("anInt", "42"))
        assertEquals(42, intLens(form))
    }

    @Test
    fun `no field should yield null`() {
        val form = bodyLens(baseRequest)
        assertEquals(null, intLens(form))
    }

    @Test
    fun `empty string yield parse to null`() {
        val form = bodyLens(baseRequest.form("anInt", ""))
        assertNull(intLens(form)) // throws LensFailure: formData 'anInt' must be integer
    }
}
