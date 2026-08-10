package org.http4k.connect.amazon.iotjobsdataplane.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.maxLength
import dev.forkhandles.values.regex

/**
 * A job identifier as the jobs data plane accepts it: alphanumerics, "-" and "_", or the
 * reserved literal `$next` which addresses the device's next pending execution.
 */
class JobId private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<JobId>(::JobId, 64.maxLength.and("[a-zA-Z0-9_-]+|\\\$next".regex)) {
        val NEXT = of("\$next")
    }
}
