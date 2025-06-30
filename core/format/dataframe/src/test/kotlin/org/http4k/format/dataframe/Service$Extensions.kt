@file:Suppress("UNCHECKED_CAST", "USELESS_CAST")
package org.http4k.format.dataframe
import org.jetbrains.kotlinx.dataframe.ColumnsScope
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataRow

val DataRow<Service>._links: DataRow<Service1> @JvmName("Service__links") get() = this["_links"] as DataRow<Service1>
val ColumnsScope<Service>.name: DataColumn<String> @JvmName("Service_name") get() = this["name"] as DataColumn<String>
val DataRow<Service>.name: String @JvmName("Service_name") get() = this["name"] as String
val ColumnsScope<Service?>.name: DataColumn<String?> @JvmName("NullableService_name") get() = this["name"] as DataColumn<String?>
val DataRow<Service?>.name: String? @JvmName("NullableService_name") get() = this["name"] as String?
