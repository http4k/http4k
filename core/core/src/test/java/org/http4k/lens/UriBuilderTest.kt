package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class UriBuilderTest {

    @Test
    fun `can inject typesafe args into a Uri`() {
        val time = Path.localDate().of("time")
        val id = Path.uuid().of("id")
        val sort = Query.boolean().required("sort")

        assertThat(
            Uri.of("/{id}/{time}").with(time of LocalDate.EPOCH, id of UUID(0, 0), sort of true),
            equalTo(Uri.of("/00000000-0000-0000-0000-000000000000/1970-01-01?sort=true"))
        )
    }
}
