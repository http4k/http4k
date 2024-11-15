package org.http4k.connect.amazon.s3

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface S3Action<R> : Action<Result<R, RemoteFailure>>
