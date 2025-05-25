package org.http4k.util

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.util.OperatingSystem.Linux
import org.http4k.util.OperatingSystem.MacOS
import org.http4k.util.OperatingSystem.Windows
import org.junit.jupiter.api.Test

class OperatingSystemTest {

    @Test
    fun `detect OS`() = runBlocking {
        System.setProperty("os.name", "Mac OS foo")
        assertThat(OperatingSystem.detect(), equalTo(MacOS))
        System.setProperty("os.name", "Windows foo")
        assertThat(OperatingSystem.detect(), equalTo(Windows))
        System.setProperty("os.name", "Some wacky distro")
        assertThat(OperatingSystem.detect(), equalTo(Linux))
    }
}
