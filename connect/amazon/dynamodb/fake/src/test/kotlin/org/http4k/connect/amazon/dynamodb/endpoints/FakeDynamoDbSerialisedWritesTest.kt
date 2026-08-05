package org.http4k.connect.amazon.dynamodb.endpoints

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.FakeDynamoDb
import org.http4k.connect.amazon.dynamodb.attrN
import org.http4k.connect.amazon.dynamodb.attrS
import org.http4k.connect.amazon.dynamodb.batchWriteItem
import org.http4k.connect.amazon.dynamodb.createTable
import org.http4k.connect.amazon.dynamodb.deleteItem
import org.http4k.connect.amazon.dynamodb.model.BillingMode
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.KeySchema
import org.http4k.connect.amazon.dynamodb.model.ReqWriteItem
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.asAttributeDefinition
import org.http4k.connect.amazon.dynamodb.model.compound
import org.http4k.connect.amazon.dynamodb.putItem
import org.http4k.connect.amazon.dynamodb.sample
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Two things need proving. The first pair of tests instrument the storage to show that every access
 * a write path makes is taken under the storage monitor. On their own they would also pass an
 * implementation which locked the read and the write separately and so still lost updates, which is
 * what the second pair rule out.
 */
class FakeDynamoDbSerialisedWritesTest {

    private val table = TableName.sample()
    private val storage = MonitoredStorage()
    private val dynamo = FakeDynamoDb(storage).client()
    private val driverWrites = 240
    private val backOffRungs = 12
    private val counter = "competitor-counter"

    @Test
    fun `single item writes access the storage under its monitor`() {
        createTable()

        storage.recording = true
        dynamo.putItem(table, Item(attrS of "hash1")).successValue()
        dynamo.deleteItem(table, Key(attrS of "hash1")).successValue()

        assertThat(storage.unlockedAccesses, equalTo(emptyList()))
    }

    @Test
    fun `batch writes access the storage under its monitor`() {
        createTable()

        storage.recording = true
        dynamo.batchWriteItem(
            mapOf(table to listOf(ReqWriteItem.Put(Item(attrS of "hash1"))))
        ).successValue()

        assertThat(storage.unlockedAccesses, equalTo(emptyList()))
    }

    @Test
    fun `no competing write can land inside a single item write`() = assertWriteIsAtomic {
        dynamo.deleteItem(table, Key(attrS of "absent")).successValue()
    }

    @Test
    fun `no competing write can land inside a batch write`() = assertWriteIsAtomic {
        dynamo.batchWriteItem(mapOf(table to listOf(ReqWriteItem.Delete(Key(attrS of "absent"))))).successValue()
    }

    /**
     * Races a competitor which is let into contention at the instant the write path has read but
     * not yet written. Code holding the monitor across both shuts the competitor out until the
     * write has landed; code releasing it in between lets the competitor complete a whole
     * read-modify-write inside that gap, which shows up as the write path writing back a table it
     * read before the competitor's, and as the competitor's increment being rolled back with it.
     *
     * The gap is only nanoseconds wide, so the competitor has to still be spinning for the monitor
     * when it opens - once it has parked, the write path always reclaims the monitor first. Hence
     * the driver deletes a key which is not there: a full read-modify-write which marshals nothing
     * back, so it holds the monitor for the shortest stretch any of these endpoints does. The
     * back-off ladder then covers the range of arrival times, the useful one depending on how fast
     * the machine is. Losing the race costs a run its chance to catch the regression, but can never
     * report one which is not there.
     */
    private fun assertWriteIsAtomic(write: () -> Unit) {
        createTable()

        val driver = Thread.currentThread()
        val contend = AtomicBoolean(false)
        val racing = AtomicBoolean(true)
        storage.onRead = { if (Thread.currentThread() === driver) contend.set(true) }
        storage.recording = true

        var competingWrites = 0
        val competitor = thread(name = "competing writer") {
            while (racing.get()) {
                // Spinning rather than parking keeps wake-up latency out of the window being aimed at.
                while (racing.get() && !contend.getAndSet(false)) Thread.onSpinWait()
                if (!racing.get()) break
                repeat(1 shl (competingWrites % backOffRungs)) { Thread.onSpinWait() }
                synchronized(storage) {
                    val stored = storage[table.value]!!
                    storage[table.value] = stored.withItem(Item(attrS of counter, attrN of (stored.counted() + 1)))
                }
                competingWrites++
            }
        }

        repeat(driverWrites) { write() }
        racing.set(false)
        competitor.join()
        storage.recording = false

        assertThat("competing writes made", competingWrites > 0, equalTo(true))
        assertThat("surviving competing increments", storage[table.value]!!.counted(), equalTo(competingWrites))
        assertThat("writes made against a table read before another write", storage.staleWrites, equalTo(0))
        assertThat(storage.unlockedAccesses, equalTo(emptyList()))
    }

    private fun DynamoTable.counted() = retrieve(Key(attrS of counter))?.let { attrN(it) } ?: 0

    private fun createTable() = dynamo.createTable(
        table,
        KeySchema = KeySchema.compound(attrS.name),
        AttributeDefinitions = listOf(attrS.asAttributeDefinition()),
        BillingMode = BillingMode.PAY_PER_REQUEST
    ).successValue()
}

private class MonitoredStorage(
    private val delegate: Storage<DynamoTable> = Storage.InMemory()
) : Storage<DynamoTable> by delegate {

    /** Set once the table exists - creating it is not one of the serialised paths. */
    var recording = false

    /** Called after a read has been served, while whichever write path asked for it is mid-flight. */
    var onRead: () -> Unit = {}

    val unlockedAccesses = mutableListOf<String>()

    /** Writes made against a table which some other write had already replaced since it was read. */
    var staleWrites = 0
        private set

    private var writes = 0
    private val writesSeenByRead = ThreadLocal<Int?>()

    override fun get(key: String): DynamoTable? {
        record("get")
        return delegate[key].also {
            if (recording) {
                writesSeenByRead.set(writes)
                onRead()
            }
        }
    }

    override fun set(key: String, data: DynamoTable) {
        record("set")
        if (recording) {
            val seenByRead = writesSeenByRead.get()
            if (seenByRead != null && seenByRead != writes) staleWrites++
            writesSeenByRead.remove()
            writes++
        }
        delegate[key] = data
    }

    private fun record(operation: String) {
        if (recording && !Thread.holdsLock(this)) unlockedAccesses += operation
    }
}
