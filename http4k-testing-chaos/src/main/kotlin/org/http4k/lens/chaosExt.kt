package org.http4k.lens

val Header.CHAOS; get() = Header.required("x-http4k-chaos")
