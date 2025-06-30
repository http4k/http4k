@file:Suppress("UNCHECKED_CAST", "USELESS_CAST")

package org.http4k.format.dataframe

import org.jetbrains.kotlinx.dataframe.DataRow

val DataRow<Repository>.fullName: String @JvmName("Repository_fullName") get() = this["full_name"] as String
val DataRow<Repository>.htmlUrl: java.net.URL @JvmName("Repository_htmlUrl") get() = this["html_url"] as java.net.URL
val DataRow<Repository>.stargazersCount: Int @JvmName("Repository_stargazersCount") get() = this["stargazers_count"] as Int
