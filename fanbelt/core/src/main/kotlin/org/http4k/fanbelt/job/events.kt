package org.http4k.fanbelt.job

import org.http4k.fanbelt.events.FanbeltFailure

data class JobFailed(val job: String, override val cause: Throwable) : FanbeltFailure
