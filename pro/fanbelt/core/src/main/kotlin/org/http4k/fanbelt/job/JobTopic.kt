/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.job

import org.http4k.fanbelt.bus.Interest
import org.http4k.fanbelt.bus.Topic

/**
 * Names one key of a job, as @job/key: an operation that puts it in Changes records that key for the job to run.
 */
internal class JobTopic private constructor(val job: String, val key: String) : Topic("@$job/$key") {
    override fun placedIn(tenant: String): Topic = of(value)

    companion object {
        fun of(job: String, key: String) = JobTopic(job, key)

        fun parse(value: String): JobTopic {
            val unprefixed = value.removePrefix("@")
            return JobTopic(unprefixed.substringBefore('/'), unprefixed.substringAfter('/'))
        }

        fun interest(job: String) = Interest.of("@$job")
    }
}
