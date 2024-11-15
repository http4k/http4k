package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class InputFormatOptions(
    val Csv: CsvOptions?
)
