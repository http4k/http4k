package org.http4k.connect.openfga

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.openfga.endpoints.batchCheck
import org.http4k.connect.openfga.endpoints.check
import org.http4k.connect.openfga.endpoints.createStore
import org.http4k.connect.openfga.endpoints.deleteStore
import org.http4k.connect.openfga.endpoints.getStore
import org.http4k.connect.openfga.endpoints.listObjects
import org.http4k.connect.openfga.endpoints.listStores
import org.http4k.connect.openfga.endpoints.listUsers
import org.http4k.connect.openfga.endpoints.read
import org.http4k.connect.openfga.endpoints.readAssertions
import org.http4k.connect.openfga.endpoints.readAuthorizationModel
import org.http4k.connect.openfga.endpoints.readAuthorizationModels
import org.http4k.connect.openfga.endpoints.readChanges
import org.http4k.connect.openfga.endpoints.streamedListObjects
import org.http4k.connect.openfga.endpoints.write
import org.http4k.connect.openfga.endpoints.writeAssertions
import org.http4k.connect.openfga.endpoints.writeAuthorizationModel
import org.http4k.connect.openfga.model.StoreId
import org.http4k.core.Uri
import org.http4k.routing.routes
import java.time.Clock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class FakeOpenFga(
    private val clock: Clock = Clock.systemUTC(),
    private val stores: MutableMap<StoreId, FgaStore> = ConcurrentHashMap()
) : ChaoticHttpHandler() {

    private val ids = AtomicLong()

    private val nextId = { UUID(0, ids.getAndIncrement()).toString() }

    fun client() = OpenFga.Http(Uri.of("http://localhost"), this)

    override val app = routes(
        createStore(clock, nextId, stores),
        listStores(stores),
        getStore(stores),
        deleteStore(stores),
        writeAuthorizationModel(nextId, stores),
        readAuthorizationModels(stores),
        readAuthorizationModel(stores),
        writeAssertions(stores),
        readAssertions(stores),
        write(clock, stores),
        check(stores),
        batchCheck(stores),
        listObjects(stores),
        streamedListObjects(stores),
        listUsers(stores),
        read(stores),
        readChanges(stores)
    )
}

fun main() {
    FakeOpenFga().start()
}
