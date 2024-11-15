package org.http4k.connect.kafka.rest.v2.extensions

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.allValues
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.flatMapFailure
import org.http4k.connect.kafka.rest.KafkaRest
import org.http4k.connect.kafka.rest.extensions.RecordConsumer
import org.http4k.connect.kafka.rest.model.ConsumerGroup
import org.http4k.connect.kafka.rest.model.ConsumerInstance
import org.http4k.connect.kafka.rest.model.Topic
import org.http4k.connect.kafka.rest.v2.commitOffsets
import org.http4k.connect.kafka.rest.v2.consumeRecords
import org.http4k.connect.kafka.rest.v2.model.CommitOffset
import org.http4k.connect.kafka.rest.v2.model.RecordFormat
import org.http4k.connect.kafka.rest.v2.model.SeekOffset
import org.http4k.connect.kafka.rest.v2.seekOffsets

/**
 * Process Records on a Topic using the passed RecordConsumer, and either commits the Offset after processing each.
 */
inline fun <reified T : Any> KafkaRest.consumeAndCommitRecords(
    group: ConsumerGroup,
    consumerInstance: ConsumerInstance,
    topic: Topic,
    recordFormat: RecordFormat,
    recordConsumer: RecordConsumer<T>
) = consumeRecords(group, consumerInstance, recordFormat)
    .flatMap {
        it
            ?.map { record ->
                when (record.value) {
                    is T -> recordConsumer(record.value)
                        .flatMap {
                            commitOffsets(
                                group, consumerInstance,
                                listOf(CommitOffset(topic, record.partition, record.offset))
                            )
                        }
                        .flatMapFailure { f ->
                            seekOffsets(
                                group, consumerInstance, listOf(
                                    SeekOffset(topic, record.partition, record.offset)
                                )
                            ).flatMap { Failure(f) }
                        }

                    else -> Failure("Wrong type of record! Expecting ${T::class.java}")
                }
            }
            ?.allValues() ?: Success(null)
    }
