package org.http4k.core

import org.http4k.lens.LensExtractor
import org.http4k.lens.LensInjector

interface Store<OUT> : LensInjector<OUT, Request>, LensExtractor<Request, OUT> {
    fun remove(value: OUT): OUT?
}
