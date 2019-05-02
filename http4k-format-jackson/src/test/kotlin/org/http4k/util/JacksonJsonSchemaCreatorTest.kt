package org.http4k.util

import org.http4k.core.Uri
import org.http4k.format.Jackson
import org.junit.jupiter.api.Test
import java.time.Duration

data class Nested(val duration: Duration)
data class Root(val uri: Uri?, val num: Int, val boolean: Boolean, val nested: Nested)

class JacksonJsonSchemaCreatorTest {

    @Test
    fun `generates schema`() {
        println(
            JacksonJsonSchemaCreator(Jackson).toSchema(
                Root(
                    Uri.of("http://uri:8000"),
                    1234,
                    false,
                    Nested(Duration.ofMillis(1000))
                ), "override")
        )
    }
}