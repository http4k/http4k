package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class LastModifiedTest {

    @Test
    fun `roundtrip last modified`() {
        val value = LastModified.parse("Thu, 01 Jan 1970 00:00:00 GMT")
        assertThat(value.toHeaderValue(), equalTo("Thu, 01 Jan 1970 00:00:00 GMT"))
        assertThat(value.value, equalTo(ZonedDateTime.of(LocalDate.EPOCH, LocalTime.MIDNIGHT, ZoneId.of("GMT"))))
    }
}
