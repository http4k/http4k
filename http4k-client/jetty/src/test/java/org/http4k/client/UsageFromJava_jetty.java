package org.http4k.client;

import org.http4k.core.HttpHandler;

public interface UsageFromJava_jetty {
    HttpHandler apache = JettyClient.create();
}
