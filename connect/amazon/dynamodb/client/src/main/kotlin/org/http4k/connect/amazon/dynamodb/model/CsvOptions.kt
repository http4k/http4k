package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class CsvOptions(
    val Delimiter: Char? = null,
    val HeaderList: List<String>? = null
)
