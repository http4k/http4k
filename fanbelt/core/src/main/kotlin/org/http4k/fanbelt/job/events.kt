package org.http4k.fanbelt.job

import org.http4k.fanbelt.events.FanbeltFailure

data class JobFailed(val job: String, val key: Any?, override val cause: Throwable) : FanbeltFailure
