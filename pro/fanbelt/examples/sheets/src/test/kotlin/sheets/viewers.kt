package sheets

import sheets.domain.CellRef
import sheets.domain.ViewerId
import java.util.UUID

fun viewerNamed(name: String) = ViewerId.of(UUID.nameUUIDFromBytes(name.toByteArray()))

private val cursorRule = Regex("""(body\S*) td:has\(#cell-(\w+)\)::after \{ content: "([^"]*)"; }""")

private val excluded = Regex("""data-viewer="([^"]+)"""")

fun cursorsIn(style: String, me: ViewerId) = cursorRule.findAll(style)
    .filter { rule -> applies(rule.groupValues[1], me) }
    .associate { rule -> CellRef.of(rule.groupValues[2]) to rule.groupValues[3].split(", ").toSet() }

private fun applies(body: String, me: ViewerId) =
    if (body.startsWith("body:not")) excluded.findAll(body).none { it.groupValues[1] == me.toString() }
    else body == """body[data-viewer="$me"]"""