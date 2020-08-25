package org.http4k.junit;

import org.http4k.core.Response;
import org.http4k.core.Status;
import org.http4k.servirtium.InteractionStorage;

public interface UsageFromJava_servirtium {
    ServirtiumRecording servirtiumRecording = new ServirtiumRecording("", req -> Response.Companion.create(Status.ACCEPTED), InteractionStorage.InMemory());
    ServirtiumReplay servirtiumReplay = new ServirtiumReplay("", InteractionStorage.InMemory());
}
