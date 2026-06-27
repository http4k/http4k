/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.model

/**
 * The outcome of a verified assertion: the new signature counter (for clone detection) and the credential's
 * current [backupState] (whether it is currently synced/backed up), which can change between sign-ins.
 */
data class AuthenticationVerdict(val signCount: Long, val backupState: Boolean = false)
