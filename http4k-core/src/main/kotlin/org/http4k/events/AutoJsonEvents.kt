package org.http4k.events

import org.http4k.format.AutoMarshallingJson

object AutoJsonEvents {
    operator fun invoke(json: AutoMarshallingJson, print: (String) -> Unit = ::println) = object : Events {
        override fun invoke(p1: Event) = print(json.asString(p1))
    }
}
