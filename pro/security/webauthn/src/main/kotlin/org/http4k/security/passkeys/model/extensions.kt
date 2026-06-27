/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.model

/** Client extension inputs the server requests at registration. `credProps` asks the browser to report
 *  whether a discoverable (resident) credential was actually created. */
data class RegistrationExtensions(val credProps: Boolean = true)

/** Client extension outputs the browser returns on the registration response. */
data class ClientExtensionResults(val credProps: CredProps? = null)

/** credProps output: [rk] = was a discoverable (resident) key created? */
data class CredProps(val rk: Boolean? = null)
