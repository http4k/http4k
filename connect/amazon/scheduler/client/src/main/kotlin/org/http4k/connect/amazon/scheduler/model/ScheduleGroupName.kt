package org.http4k.connect.amazon.scheduler.model

import dev.forkhandles.values.NonEmptyStringValueFactory
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.ResourceId

class ScheduleGroupName private constructor(value: String) : ResourceId(value) {
    companion object : NonEmptyStringValueFactory<ScheduleGroupName>(::ScheduleGroupName) {
        fun of(arn: ARN) = of(arn.value)
    }
}

