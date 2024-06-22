@file:ImportDataSchema(
    "Service",
    path = "src/test/resources/services.json",
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

class DataFrameJsonTests {
    @Test
    fun `can extract a set of typed JSON records from an HTTP message`() {
        val request = Request(GET, "").body(javaClass.getResourceAsStream("/services.json")!!)
        val services = request.dataFrameJson<Service>()
        assertThat(services.size(), equalTo(DataFrameSize(2, 2)))
        assertThat(services[0].name, equalTo("service_a"))
        assertThat(services[0]._links.url, equalTo("https://a"))
    }
}
