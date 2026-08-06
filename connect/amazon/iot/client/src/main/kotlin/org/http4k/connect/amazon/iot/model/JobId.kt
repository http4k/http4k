package org.http4k.connect.amazon.iot.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.maxLength
import dev.forkhandles.values.regex

/**
 * A job identifier, unique within the account. AWS allows only alphanumerics, "-" and "_",
 * which is what lets it be dropped straight into the `/jobs/{jobId}` path.
 */
class JobId private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<JobId>(::JobId, 64.maxLength.and("[a-zA-Z0-9_-]+".regex))
}
