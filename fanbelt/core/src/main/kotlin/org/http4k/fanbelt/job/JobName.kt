package org.http4k.fanbelt.job

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

/**
 * The name a job's recorded work carries.
 */
class JobName private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<JobName>(::JobName, "[A-Za-z][A-Za-z0-9_-]{0,62}".regex)
}
