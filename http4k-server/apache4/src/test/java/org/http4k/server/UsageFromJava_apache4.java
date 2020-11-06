package org.http4k.server;

import org.http4k.core.Response;
import org.http4k.core.Status;

import static org.http4k.server.Http4kServerKt.asServer;

public interface UsageFromJava_apache4 {
    Apache4Server apache = new Apache4Server(8000);
    Http4kServer http4kServer = asServer(req -> Response.Companion.create(Status.ACCEPTED), apache);
}
