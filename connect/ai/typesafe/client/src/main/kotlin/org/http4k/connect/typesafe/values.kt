package org.http4k.connect.typesafe

import dev.forkhandles.values.DoubleValue
import dev.forkhandles.values.DoubleValueFactory
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.Validation

class QuestionId private constructor(value: String) : StringValue(value), Comparable<QuestionId> {
    override fun compareTo(other: QuestionId) = value.compareTo(other.value)

    companion object : NonBlankStringValueFactory<QuestionId>(::QuestionId)
}

class Probability private constructor(value: Double) : DoubleValue(value) {
    companion object : DoubleValueFactory<Probability>(::Probability, zeroToOne)
}

class Confidence private constructor(value: Double) : DoubleValue(value) {
    companion object : DoubleValueFactory<Confidence>(::Confidence, zeroToOne)
}

private val zeroToOne: Validation<Double> = { value: Double -> value in 0.0..1.0 }
