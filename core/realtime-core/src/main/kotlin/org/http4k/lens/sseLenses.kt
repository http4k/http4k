package org.http4k.lens

val Header.LAST_EVENT_ID get() = Header.optional("Last-Event-ID")
