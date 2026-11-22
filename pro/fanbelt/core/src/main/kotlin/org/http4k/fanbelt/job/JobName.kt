/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.job

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

/**
 * The name a job's recorded rows carry. Give it explicitly to rename the job's property without stranding waiting work.
 */
class JobName private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<JobName>(::JobName, "[A-Za-z][A-Za-z0-9_-]{0,62}".regex)
}
