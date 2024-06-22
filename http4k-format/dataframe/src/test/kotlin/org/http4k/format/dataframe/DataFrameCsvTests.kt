@file:ImportDataSchema(
    "Repository",
    path = "src/test/resources/repositories.csv",
)

package org.http4k.format.dataframe

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.jetbrains.kotlinx.dataframe.annotations.ImportDataSchema
import org.jetbrains.kotlinx.dataframe.impl.DataFrameSize
import org.jetbrains.kotlinx.dataframe.size
import org.junit.jupiter.api.Test

class DataFrameCsvTests {
    @Test
    fun `can extract a set of typed CSV records from an HTTP message`() {
        val request = Request(GET, "").body(javaClass.getResourceAsStream("/repositories.csv")!!)
        val repos = request.dataFrameCsv<Repository>()
        assertThat(repos.size(), equalTo(DataFrameSize(5, 4)))
        assertThat(repos[0].fullName, equalTo("JPS"))
        assertThat(repos[0].htmlUrl.toString(), equalTo("https://github.com/JPS"))
        assertThat(repos[0].stargazersCount, equalTo(23))
    }
}
