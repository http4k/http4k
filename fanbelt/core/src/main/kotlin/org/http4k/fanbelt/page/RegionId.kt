package org.http4k.fanbelt.page

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class RegionId private constructor(value: String) : StringValue(value) {
    internal fun placeholder() = """<div id="$value"></div>"""
    companion object : StringValueFactory<RegionId>(::RegionId, "[A-Za-z][A-Za-z0-9_-]*".regex)
}
