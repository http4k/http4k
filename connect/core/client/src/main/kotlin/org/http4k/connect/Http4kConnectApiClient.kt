package org.http4k.connect

@Target(AnnotationTarget.CLASS)
annotation class Http4kConnectApiClient

@Deprecated("Renamed to Http4kConnectApiClient instead", ReplaceWith("Http4kConnectApiClient"))
typealias Http4kConnectAdapter = Http4kConnectApiClient

/**
 * Marker attached to all actions to drive the client code generation.
 *
 * docs: Optional information for this action. Can be link or other notes.
 */
@Target(AnnotationTarget.CLASS)
annotation class Http4kConnectAction(val docs: String = "")
