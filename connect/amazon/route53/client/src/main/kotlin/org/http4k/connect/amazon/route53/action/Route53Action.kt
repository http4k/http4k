package org.http4k.connect.amazon.route53.action

import dev.forkhandles.result4k.Result4k
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface Route53Action<R> : Action<Result4k<R, RemoteFailure>>
