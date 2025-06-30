package org.http4k.format.dataframe

import org.jetbrains.kotlinx.dataframe.ColumnsScope
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.columns.ColumnGroup
import org.jetbrains.kotlinx.dataframe.annotations.ColumnName
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.io.readCsv

@DataSchema
interface Repository {
    @ColumnName("full_name")
    val fullName: String
    @ColumnName("html_url")
    val htmlUrl: java.net.URL
    @ColumnName("stargazers_count")
    val stargazersCount: Int
    val topics: String
    val watchers: Int
    public companion object {
      public const val defaultPath: kotlin.String = "src/test/resources/repositories.csv"

      public val defaultDelimiter: kotlin.Char = ','

      public fun readCsv(
        path: kotlin.String = defaultPath,
        delimiter: kotlin.Char = defaultDelimiter,
        verify: kotlin.Boolean? = null,
      ): org.jetbrains.kotlinx.dataframe.DataFrame<Repository> {
        val df = DataFrame.readCsv(path, delimiter = delimiter)
        return if (verify != null) df.cast(verify = verify) else df.cast()
      }
    }

}
