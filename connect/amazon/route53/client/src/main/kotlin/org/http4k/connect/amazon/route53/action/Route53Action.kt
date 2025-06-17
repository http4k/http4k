package org.http4k.connect.amazon.route53.action

interface Route53Action<R> : Action<Result<R, RemoteFailure>>
