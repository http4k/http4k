package org.http4k.fanbelt.testing

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory

enum class Colour { Red }

class Shade private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<Shade>(::Shade, { it.startsWith("deep") })
}

val elementReadsPage = """<html><body>
    <span id="shade" data-shade="deep blue" data-colour="Red" data-bad="pale" data-other="Blue">deep red</span>
    <span id="colour">Red</span>
    <span id="neither">pale</span>
</body></html>"""
