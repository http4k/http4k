package org.http4k.connect.amazon.cloudwatch.model

import dev.forkhandles.values.AbstractComparableValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class PercentileExtendedStatistic private constructor(value: String) : AbstractComparableValue<PercentileExtendedStatistic, String>(value) {
    companion object : StringValueFactory<PercentileExtendedStatistic>(::PercentileExtendedStatistic, "p((0|[1-9]|[1-9]\\d)(\\.\\d)?|100(\\.0)?)".regex)
}
