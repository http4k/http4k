package org.http4k.connect.amazon.cloudwatch.model

import dev.forkhandles.values.AbstractComparableValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class ExtendedStatistic private constructor(value: String) : AbstractComparableValue<ExtendedStatistic, String>(value) {
    companion object : StringValueFactory<ExtendedStatistic>(::ExtendedStatistic, "((p|tm|tc|ts|wm)90|IQM|PR(\\((0|[1-9]\\d+):(0|[1-9]\\d+)\\))|(TC|TM|TS|WM)\\((([1-8]\\d|90)%)?:(([1-8]\\d|90)%)?\\))".regex)
}
