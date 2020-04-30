package org.http4k.openapi.client

import com.squareup.kotlinpoet.FileSpec
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.OpenApi3Spec

object ClientApiGenerator : ApiGenerator {
    override fun invoke(p1: OpenApi3Spec) = emptyList<FileSpec>()

}
