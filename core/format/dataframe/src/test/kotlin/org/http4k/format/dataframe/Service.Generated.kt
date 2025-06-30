package org.http4k.format.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema

@DataSchema(isOpen = false)
interface Service1 {
    val url: String
}

@DataSchema
interface Service {
    val _links: Service1
    val name: String
}
