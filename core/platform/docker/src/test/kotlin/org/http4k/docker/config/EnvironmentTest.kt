package org.http4k.docker.config

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.config.Environment
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.writeText

class EnvironmentTest {

    @TempDir
    var temp: Path? = null

    @Test
    fun `reads secrets files`() {

        temp!!. also { t ->
            (t / "secret-one").writeText("secret1\n")
            (t / "secret-t-w-o").writeText("secret2\n")
            (t / "aws_access_key_id").writeText("very secret\n")

            val env = Environment.fromDockerSwarmSecrets(t)

            assertThat(env["SECRET_ONE"], equalTo("secret1"))
            assertThat(env["SECRET_T_W_O"], equalTo("secret2"))
            assertThat(env["AWS_ACCESS_KEY_ID"], equalTo("very secret"))
        }
    }
}
