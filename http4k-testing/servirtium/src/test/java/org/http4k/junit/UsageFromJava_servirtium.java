package org.http4k.junit;

import org.http4k.client.ApacheClient;
import org.http4k.core.Response;
import org.http4k.core.Status;
import org.http4k.core.Uri;
import org.http4k.server.SunHttp;
import org.http4k.servirtium.InteractionOptions;
import org.http4k.servirtium.InteractionStorage;
import org.http4k.servirtium.ServirtiumServer;

public interface UsageFromJava_servirtium {
    // junit extension
    ServirtiumRecording servirtiumRecording = new ServirtiumRecording("", req -> Response.Companion.create(Status.ACCEPTED), InteractionStorage.InMemory());
    ServirtiumReplay servirtiumReplay = new ServirtiumReplay("", InteractionStorage.InMemory());

    // standalone server
    ServirtiumServer recordingServer = ServirtiumServer.Recording(
        "name of interaction",
        Uri.of("http://localhost:8000"),
        InteractionStorage.InMemory(),
        InteractionOptions.Defaults,
        9000,
        SunHttp::new,
        ApacheClient.create()
    );

    ServirtiumServer replayServer = ServirtiumServer.Replay(
        "name of interaction",
        InteractionStorage.InMemory(),
        InteractionOptions.Defaults,
        9000,
        SunHttp::new
    );
}
