@file:Suppress("UNCHECKED_CAST", "USELESS_CAST")
package org.http4k.format.dataframe
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.ColumnsScope
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.columns.ColumnGroup

val ColumnsScope<org.http4k.format.dataframe.Service1>.url: DataColumn<String> @JvmName("Service1_url") get() = this["url"] as DataColumn<String>
val DataRow<org.http4k.format.dataframe.Service1>.url: String @JvmName("Service1_url") get() = this["url"] as String
val ColumnsScope<org.http4k.format.dataframe.Service1?>.url: DataColumn<String?> @JvmName("NullableService1_url") get() = this["url"] as DataColumn<String?>
val DataRow<org.http4k.format.dataframe.Service1?>.url: String? @JvmName("NullableService1_url") get() = this["url"] as String?
