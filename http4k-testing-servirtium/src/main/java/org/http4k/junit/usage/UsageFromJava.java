package org.http4k.junit.usage;

import org.http4k.junit.ServirtiumRecording;
import org.http4k.junit.ServirtiumReplay;
import org.http4k.server.SunHttp;
import org.http4k.servirtium.InteractionStorage;

public class UsageFromJava {
    static {
        new ServirtiumRecording("", SunHttp::new, InteractionStorage.InMemory());
        new ServirtiumReplay("", InteractionStorage.InMemory());
    }
}
