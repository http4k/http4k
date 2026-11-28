package org.http4k.fanbelt.job

import org.http4k.fanbelt.bus.Interest
import org.http4k.fanbelt.bus.Topic

/**
 * The topic that names one key of a job.
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
