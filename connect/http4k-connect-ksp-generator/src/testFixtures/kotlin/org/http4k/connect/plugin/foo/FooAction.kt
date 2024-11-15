package org.http4k.connect.plugin.foo

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface FooAction<R> : Action<Result<R, RemoteFailure>>
