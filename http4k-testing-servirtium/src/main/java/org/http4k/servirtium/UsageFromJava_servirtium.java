package org.http4k.servirtium;

import kotlin.jvm.functions.Function1;
import org.http4k.core.Credentials;
import org.http4k.core.Uri;

import java.io.File;

public interface UsageFromJava_servirtium {
    GitHub gitHub = new GitHub("", "", new Credentials("", ""));

    Function1<String, InteractionStorage> memoryStorage = InteractionStorage.InMemory();

    InteractionControl interactionControl = InteractionControl.StorageBased(memoryStorage.invoke(""));
    Function1<String, InteractionStorage> diskStorage = InteractionStorage.Disk(new File("."));

    ServirtiumServer recording = ServirtiumServer.Recording("name", Uri.of("foobar"), diskStorage);

    ServirtiumServer replay = ServirtiumServer.Replay("name", memoryStorage);
}
