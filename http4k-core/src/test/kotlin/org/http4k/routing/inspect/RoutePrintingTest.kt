package org.http4k.routing.inspect

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RouterDescription
import org.http4k.routing.RouterMatch
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.MethodNotMatched
import org.http4k.routing.RouterMatch.Unmatched
import org.http4k.routing.bind
import org.http4k.routing.inspect.EscapeMode.Ansi
import org.http4k.routing.inspect.EscapeMode.Pseudo
import org.http4k.routing.inspect.ForegroundColour.Cyan
import org.http4k.routing.inspect.ForegroundColour.Green
import org.http4k.routing.inspect.ForegroundColour.Red
import org.http4k.routing.inspect.Variation.Strikethrough
import org.http4k.routing.routes
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


@ExtendWith(ApprovalTest::class)
class RoutePrintingTest {

    private val routes = routes(
        "/a" bind Method.GET to { Response(Status.OK).body("matched a") },
        "/b/c" bind routes(
            "/d" bind Method.GET to { Response(Status.OK).body("matched b/c/d") },
            "/e" bind routes(
                "/f" bind Method.GET to { Response(Status.OK).body("matched b/c/e/f") },
                "/g" bind routes(
                    Method.GET to { _: Request -> Response(Status.OK).body("matched b/c/e/g/GET") },
                    Method.POST to { _: Request -> Response(Status.OK).body("matched b/c/e/g/POST") }
                )
            ),
            "/" bind Method.GET to { Response(Status.OK).body("matched b/c") }
        )
    )

    @Test
    fun `describe routes`(approvalTest: Approver) {
        approvalTest.assertApproved(routes.description.prettify(escapeMode = Pseudo))
    }

    @Test
    fun `describe matching`(approvalTest: Approver) {
        val request = Request(Method.POST, "/b/c/e/g")
        approvalTest.assertApproved(routes.match(request).prettify(escapeMode = Pseudo))
    }
}

fun RouterDescription.prettify(depth: Int = 0, escapeMode: EscapeMode = Ansi): String = (" ".repeat(depth * 2)).let { indent ->
    val lineBreak = if (description == "or") "\n" else ""
    val indentValue = if (description == "or") indent else ""
    if (children.isEmpty()) {
        description.styled(TextStyle(Cyan), escapeMode)
    } else {
        "$lineBreak$indentValue(${children.joinToString("$lineBreak$indentValue $description ") { it.prettify(depth + 1, escapeMode) }})"
    }
}

fun RouterMatch.prettify(depth: Int = 0, escapeMode: EscapeMode = Ansi): String = (" ".repeat(depth * 2)).let { indent ->
    val lineBreak = if (description.description == "or") "\n" else ""
    val indentValue = if (description.description == "or") indent else ""
    if (subMatches.isEmpty()) {
        description.description.styled(colour, escapeMode)
    } else {
        "$lineBreak$indentValue${"(".styled(colour, escapeMode)}${subMatches.joinToString("$lineBreak$indentValue ${description.description.styled(colour, escapeMode)} ") { it.prettify(depth + 1, escapeMode) }}${")".styled(colour, escapeMode)}"
    }
}

private val RouterMatch.colour: TextStyle
    get() = when (this) {
        is MatchingHandler, is MatchedWithoutHandler -> TextStyle(Green)
        is MethodNotMatched, is Unmatched -> TextStyle(Red, variation = Strikethrough)
    }


