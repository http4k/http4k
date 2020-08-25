package org.http4k.servirtium.usage;

import org.http4k.core.Credentials;
import org.http4k.core.Uri;
import org.http4k.servirtium.GitHub;
import org.http4k.servirtium.InteractionControl;
import org.http4k.servirtium.InteractionStorage;
import org.http4k.servirtium.ServirtiumServer;

import java.io.File;

public class UsageFromJava {
    static {
        new GitHub("", "", new Credentials("", ""));

        InteractionControl.StorageBased(InteractionStorage.InMemory().invoke(""));

        ServirtiumServer.Recording("name", Uri.of("foobar"), InteractionStorage.Disk(new File(".")));

        ServirtiumServer.Replay("name", InteractionStorage.InMemory());
    }
}
