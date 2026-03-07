package org.http4k.wiretap.util

import org.http4k.core.Response
import org.http4k.format.asDatastarSignal
import org.http4k.lens.datastarSignals

interface SignalModel

fun Response.datastarSignal(model: SignalModel): Response =
    datastarSignals(Json.asDatastarSignal(model))
