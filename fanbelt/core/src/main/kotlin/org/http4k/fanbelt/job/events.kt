/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.job

import org.http4k.fanbelt.events.FanbeltFailure

data class JobFailed(val job: String, override val cause: Throwable) : FanbeltFailure
