package org.http4k.connect.amazon.instancemetadata

import dev.forkhandles.result4k.Result
import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure
import org.http4k.core.Response

interface Ec2MetadataAction<R> : Action<Result<R, RemoteFailure>>

fun <DOMAIN : Value<String>> Response.value(factory: ValueFactory<DOMAIN, String>) = bodyString()
    .lines()
    .first()
    .trim()
    .let(factory::parse)
