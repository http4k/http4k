package org.http4k.connect.amazon.iotjobsdataplane

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface IotJobsDataPlaneAction<R> : Action<Result<R, RemoteFailure>>
