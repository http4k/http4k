package org.http4k.security.oauth.server.request

import com.natpryce.Result
import com.natpryce.Failure
import com.natpryce.Success
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.apache.commons.codec.binary.Base64
import org.http4k.security.oauth.server.InvalidRequestObject
import org.http4k.security.oauth.server.request.RequestObjectExtractor.extractRequestJwtClaimsAsMap
import org.junit.jupiter.api.Test

internal class RequestObjectExtractorTest {

    @Test
    fun `if not three parts then failure`() {
        assertThat(extractRequestJwtClaimsAsMap("kasdjflksadjfsjdfaksjdf"), equalTo(failure()))
        assertThat(extractRequestJwtClaimsAsMap("kasdjflksadj.fsjdfaksjdf"), equalTo(failure()))
        assertThat(extractRequestJwtClaimsAsMap("kasdjfl.ksadj.fsjdfa.ksjdf"), equalTo(failure()))
    }

    @Test
    fun `if has three parts but middle part is not valid base64 encoded`() {
        assertThat(extractRequestJwtClaimsAsMap("kasdjfl.ksadjfsjd.faksjdf"), equalTo(failure()))
    }

    @Test
    fun `if middle part is correctly base64 encoded but not json then error`() {
        assertThat(extractRequestJwtClaimsAsMap("kasdjfl.${Base64.encodeBase64String("something not json".toByteArray())}.faksjdf"),
            equalTo(failure()))
    }

    @Test
    fun `if middle part is correctly base64 encoded json then success`() {
        assertThat(extractRequestJwtClaimsAsMap("kasdjfl.${Base64.encodeBase64String("{\"foo\":\"bar\"}".toByteArray())}.faksjdf"),
            equalTo(success(mapOf("foo" to "bar"))))
    }

    private fun failure(): Result<Map<*, *>, InvalidRequestObject> = Failure(InvalidRequestObject)
    private fun success(data: Map<*, *>): Result<Map<*, *>, InvalidRequestObject> = Success(data)

}
