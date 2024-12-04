package org.http4k.connect

import kotlin.annotation.AnnotationTarget.CLASS

@Target(CLASS)
annotation class Http4kConnectApiClient

/**
 * Marker attached to all actions to drive the client code generation.
 *
 * docs: Optional information for this action. Can be link or other notes.
 */
@Target(CLASS)
annotation class Http4kConnectAction(val docs: String = "")
