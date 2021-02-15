package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.lens.Header.CONTENT_TYPE
import org.junit.jupiter.api.Test

class LensFailureTest {
    private val unsupported = Unsupported(CONTENT_TYPE.meta)
    private val invalid = Invalid(Meta(true, "query", ParamMeta.BooleanParam, "name"))
    private val missing = Missing(Meta(true, "header", ParamMeta.BooleanParam, "name"))

    @Test
    fun `overall returns invalid if there are no failures`() {
        assertThat(LensFailure(target = Request(GET, "")).overall(), equalTo(Failure.Type.Invalid))
    }

    @Test
    fun `overall returns unsupported if there any unsupported failures`() {
        assertThat(LensFailure(unsupported, invalid, missing, target = Request(GET, "")).overall(), equalTo(Failure.Type.Unsupported))
    }

    @Test
    fun `overall returns invalid if there only invalid and missing failures`() {
        assertThat(LensFailure(unsupported, invalid, missing, target = Request(GET, "")).overall(), equalTo(Failure.Type.Unsupported))
    }

    @Test
    fun `overall returns missing if there no invalid for unsupported ailures`() {
        assertThat(LensFailure(missing, missing, target = Request(GET, "")).overall(), equalTo(Failure.Type.Missing))
    }

    @Test
    fun `failures have descriptive toString`() {
        assertThat(missing.toString(), equalTo("header 'name' is required"))
        assertThat(invalid.toString(), equalTo("query 'name' must be boolean"))
        assertThat(unsupported.toString(), equalTo("header 'content-type' is not acceptable"))
    }
}
