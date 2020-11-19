package org.http4k.server;

import org.http4k.core.Response;
import org.http4k.core.Status;

import static org.http4k.server.Http4kServerKt.asServer;

public interface UsageFromJava_ktorcio {
    KtorCIO ktorcio = new KtorCIO(8000);
    Http4kServer http4kServer = asServer(req -> Response.create(Status.ACCEPTED), ktorcio);
}
