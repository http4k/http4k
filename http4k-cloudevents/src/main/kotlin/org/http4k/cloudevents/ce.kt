package org.http4k.cloudevents

import io.cloudevents.CloudEvent
import org.http4k.core.Response

typealias CEHandler = (CloudEvent) -> Response
