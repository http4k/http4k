package org.http4k.format.dataframe

import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.TEXT_CSV
import org.jetbrains.kotlinx.dataframe.AnyFrame
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.JsonPath
import org.jetbrains.kotlinx.dataframe.api.ParserOptions
import org.jetbrains.kotlinx.dataframe.io.AdjustCsvSpecs
import org.jetbrains.kotlinx.dataframe.io.ColType
import org.jetbrains.kotlinx.dataframe.io.Compression
import org.jetbrains.kotlinx.dataframe.io.JSON.TypeClashTactic
import org.jetbrains.kotlinx.dataframe.io.JSON.TypeClashTactic.ARRAY_AND_VALUE_COLUMNS
import org.jetbrains.kotlinx.dataframe.io.readCsv
import org.jetbrains.kotlinx.dataframe.io.readJson
import java.io.InputStream

/**
 * Represents the extraction format for taking data and converting it into a DataFrame.
 */
sealed interface DataFrameFormat : (InputStream) -> AnyFrame {
    val contentType: ContentType
}

data class CSV(
    val delimiter: Char = ',',
    val header: List<String> = emptyList(),
    val hasFixedWidthColumns: Boolean = false,
    val fixedColumnWidths: List<Int> = emptyList(),
    val colTypes: Map<String, ColType> = emptyMap(),
    val skipLines: Long = 0L,
    val readLines: Long? = null,
    val parserOptions: ParserOptions? = null,
    val ignoreEmptyLines: Boolean = false,
    val allowMissingColumns: Boolean = true,
    val ignoreExcessColumns: Boolean = true,
    val quote: Char = '"',
    val ignoreSurroundingSpaces: Boolean = true,
    val trimInsideQuoted: Boolean = false,
    val parseParallel: Boolean = true,
    val compression: Compression<*> = Compression.None,
    val adjustCsvSpecs: AdjustCsvSpecs = { it },
    override val contentType: ContentType = TEXT_CSV
) : DataFrameFormat {
    override operator fun invoke(input: InputStream) =
        DataFrame.readCsv(
            input,
            delimiter,
            header,
            hasFixedWidthColumns,
            fixedColumnWidths,
            colTypes,
            skipLines,
            readLines,
            parserOptions,
            ignoreEmptyLines,
            allowMissingColumns,
            ignoreExcessColumns,
            quote,
            ignoreSurroundingSpaces,
            trimInsideQuoted,
            parseParallel,
            compression,
            adjustCsvSpecs
        )
}

data class JSON(
    val header: List<String> = emptyList(),
    val keyValuePaths: List<JsonPath> = emptyList(),
    val typeClashTactic: TypeClashTactic = ARRAY_AND_VALUE_COLUMNS,
    override val contentType: ContentType = APPLICATION_JSON
) : DataFrameFormat {
    override operator fun invoke(input: InputStream) =
        DataFrame.readJson(input, header, keyValuePaths, typeClashTactic)
}
