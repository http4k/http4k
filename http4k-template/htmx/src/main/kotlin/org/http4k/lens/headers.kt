package org.http4k.lens

import org.http4k.core.Request

val Header.HX_REQUEST get() = Header.boolean().defaulted("HX-Request", false)
val Header.HX_TRIGGER get() = Header.optional("HX-Trigger")
val Header.HX_TRIGGER_NAME get() = Header.optional("HX-Trigger-Name")
val Header.HX_PROMPT get() = Header.optional("HX-Prompt")

val Header.HX_PUSH get() = Header.uri().optional("HX-Push")
val Header.HX_REDIRECT get() = Header.uri().optional("HX-Redirect")
val Header.HX_LOCATION get() = Header.uri().optional("HX-Location")
val Header.HX_REFRESH get() = Header.boolean().defaulted("HX-Refresh", false)
val Header.HX_TRIGGER_AFTER_SWAP get() = Header.optional("HX-Trigger-After-Swap")
val Header.HX_TRIGGER_AFTER_SETTLE get() = Header.optional("HX-Trigger-After-Settle")

fun Request.isHtmx() = Header.HX_REQUEST(this)

