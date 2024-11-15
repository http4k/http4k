package org.http4k.format.dataframe

import org.http4k.core.Body
import org.http4k.core.HttpMessage
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.binary
import org.jetbrains.kotlinx.dataframe.api.cast

/**
 * Construct Lens to extract DataFrame from the body of the message
 */
fun Body.Companion.dataFrame(
    format: DataFrameFormat,
    description: String? = null,
    contentNegotiation: ContentNegotiation = ContentNegotiation.None
) = binary(format.contentType, description, contentNegotiation).map(format)

/**
 * Read a DataFrame from the body of the message
 */
fun HttpMessage.dataFrame(options: DataFrameFormat) =
    Body.dataFrame(options).toLens()(this)

/**
 * Read a DataFrame from the body of the message as CSV and auto-cast it to the known schema
 */
fun <T> HttpMessage.dataFrameCsv() = dataFrame(CSV()).cast<T>()

/**
 * Read a DataFrame from the body of the message as JSON and auto-cast it to the known schema
 */
fun <T> HttpMessage.dataFrameJson() = dataFrame(JSON()).cast<T>()
