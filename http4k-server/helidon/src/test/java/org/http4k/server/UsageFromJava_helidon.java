package org.http4k.server;

import org.http4k.core.Response;

import static org.http4k.core.Status.ACCEPTED;
import static org.http4k.server.Http4kServerKt.asServer;

public interface UsageFromJava_helidon {
    Helidon helidon = new Helidon(8000);
    Http4kServer http4kServer = asServer(req -> Response.create(ACCEPTED), helidon);
}
