package org.http4k.server;

import org.http4k.core.Response;
import org.http4k.core.Status;

import static org.http4k.server.Http4kServerKt.asServer;

public interface UsageFromJava_ktornetty {
    KtorNetty ktorcio = new KtorNetty(8000);
    // TODO FIXME

//    Http4kServer http4kServer = asServer(req -> Response.Companion.create(Status.ACCEPTED), ktorcio);
}
