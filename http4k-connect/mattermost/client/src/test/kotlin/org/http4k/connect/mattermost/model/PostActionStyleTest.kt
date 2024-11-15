package org.http4k.connect.mattermost.model

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import dev.forkhandles.values.ofOrNull
import org.junit.jupiter.api.Test

class PostActionStyleTest {

    @Test
    fun `must be valid string`() {
        assertThat(PostActionStyle.ofOrNull(""), absent())
        assertThat(PostActionStyle.ofOrNull("123456"), absent())
        assertThat(PostActionStyle.ofOrNull("#ASDasd"), absent())
        assertThat(PostActionStyle.ofOrNull("#123456"), present())
        assertThat(PostActionStyle.ofOrNull("default"), present())
        assertThat(PostActionStyle.ofOrNull("primary"), present())
        assertThat(PostActionStyle.ofOrNull("success"), present())
        assertThat(PostActionStyle.ofOrNull("good"), present())
        assertThat(PostActionStyle.ofOrNull("warning"), present())
        assertThat(PostActionStyle.ofOrNull("danger"), present())
    }
}
