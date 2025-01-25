package org.http4k.htmx

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class HxSwap private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<HxSwap>(::HxSwap) {
        val innerHTML = of("innerHTML")
        val outerHTML = of("outerHTML")
        val beforebegin = of("beforebegin")
        val afterbegin = of("afterbegin")
        val beforeend = of("beforeend")
        val afterend = of("afterend")
        val delete = of("delete")
        val none = of("none")
    }
}
