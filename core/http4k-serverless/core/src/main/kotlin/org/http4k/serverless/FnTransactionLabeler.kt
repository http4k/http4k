package org.http4k.serverless

typealias FnTransactionLabeler<In, Out> = (FnTransaction<In, Out>) -> FnTransaction<In, Out>
