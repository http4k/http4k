package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.endsWith
import org.junit.jupiter.api.Test
import java.io.File

class FileSystemApprovalSourceTest {

    private val fs = FileSystemApprovalSource(File("src/test/resources"))

    @Test
    fun `filenames are as expected`() {
        assertThat(fs.approvedFor("hello").toString(), endsWith("/hello.approved"))
        assertThat(fs.actualFor("hello").toString(), endsWith("/hello.actual"))
    }
}
