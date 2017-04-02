package org.reekwest.http.core.stringentity

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.ok

class StringEntityTest {
    @Test
    fun can_use_string_as_entity() {
        assertThat(ok().entity("abc").entity.toString(), equalTo("abc"))
    }
}