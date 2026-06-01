package org.http4k.storyboard

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Moshi
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class StoryTest {

    @Test
    fun `empty story`(approver: Approver) {
        approver(Story("demo", emptyList()))
    }

    @Test
    fun `single frame`(approver: Approver) {
        approver(Story("demo", listOf(StoryFrame("Home", "initial load", "PGh0bWw+PC9odG1sPg=="))))
    }

    @Test
    fun `multiple frames`(approver: Approver) {
        approver(
            Story(
                "demo",
                listOf(
                    StoryFrame("first", "", "AAA"),
                    StoryFrame("second", "n", "BBB")
                )
            )
        )
    }

    @Test
    fun `escapes special characters`(approver: Approver) {
        approver(
            Story(
                "demo",
                listOf(
                    StoryFrame("a \"quoted\" title", "c:\\path", ""),
                    StoryFrame("line1\nline2", "tab\there", "")
                )
            )
        )
    }

    @Test
    fun `preserves non-ascii unicode`(approver: Approver) {
        approver(Story("demo", listOf(StoryFrame("café — 日本", "", ""))))
    }

    private operator fun Approver.invoke(story: Story) = assertApproved(
        Response(OK).with(CONTENT_TYPE of APPLICATION_JSON).body(Moshi.asFormatString(story))
    )

}
