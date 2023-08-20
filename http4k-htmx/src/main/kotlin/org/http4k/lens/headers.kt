package org.http4k.lens

import org.http4k.core.Request
import org.http4k.htmx.CssSelector
import org.http4k.htmx.HxSwap
import org.http4k.htmx.Id

val Header.HX_BOOSTED get() = Header.boolean().defaulted("HX-Boosted", false)
val Header.HX_CURRENT_URL get() = Header.uri().optional("HX-Current-Url")
val Header.HX_HISTORY_RESTORE_REQUEST get() = Header.boolean().defaulted("HX-History-Restore-Request", false)
val Header.HX_PROMPT get() = Header.optional("HX-Prompt")
val Header.HX_REQUEST get() = Header.boolean().defaulted("HX-Request", false)
val Header.HX_TARGET get() = Header.value(Id).optional("HX-Target")
val Header.HX_TRIGGER_NAME get() = Header.optional("HX-Trigger-Name")
val Header.HX_TRIGGER get() = Header.value(Id).optional("HX-Trigger")
val Header.HX_LOCATION get() = Header.uri().optional("HX-Location")
val Header.HX_PUSH_URL get() = Header.uri().optional("HX-Push-Url")
val Header.HX_REDIRECT get() = Header.uri().optional("HX-Redirect")
val Header.HX_REFRESH get() = Header.boolean().defaulted("HX-Refresh", false)
val Header.HX_REPLACE_URL get() = Header.uri().optional("HX-Replace-Url")
val Header.HX_RESWAP get() = Header.value(HxSwap).optional("HX-Reswap")
val Header.HX_RETARGET get() = Header.value(CssSelector).optional("HX-Retarget")
val Header.HX_RESELECT get() = Header.value(CssSelector).optional("HX-Reselect")
val Header.HX_TRIGGER_AFTER_SETTLE get() = Header.optional("HX-Trigger-After-Settle")
val Header.HX_TRIGGER_AFTER_SWAP get() = Header.optional("HX-Trigger-After-Swap")

fun Request.isHtmx() = Header.HX_REQUEST(this)
fun Request.isHtmxBoosted() = Header.HX_BOOSTED(this)
fun Request.isHtmxHistoryRestoreRequest() = Header.HX_HISTORY_RESTORE_REQUEST(this)

