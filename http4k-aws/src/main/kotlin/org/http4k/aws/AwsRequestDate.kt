package org.http4k.aws

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal data class AwsRequestDate(val basic: String, val full: String) {
    companion object {
        private val FORMAT_BASIC = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"))
        private val FORMAT_FULL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneId.of("UTC"))

        fun of(instant: Instant): AwsRequestDate =
            AwsRequestDate(FORMAT_BASIC.format(instant), FORMAT_FULL.format(instant))
    }
}
