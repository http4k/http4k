package org.http4k.connect.kafka.rest.model

data class ConsumerState(
    val instances: Set<ConsumerInstance>,
    val autoCommit: Boolean,
    val offsets: Map<Topic, TopicOffsetState>
) {
    fun committedRecords(topic: Topic) = offsets[topic]?.committed?.value ?: 0

    fun add(instance: ConsumerInstance) = copy(instances = instances + instance)
    fun remove(instance: ConsumerInstance) = copy(instances = instances - instance)

    fun new(topic: Topic) =
        copy(
            offsets =
            offsets + (topic to offsets.getOrDefault(topic, TopicOffsetState()))
        )

    fun next(topic: Topic, nextOffset: Offset) =
        copy(
            offsets =
            offsets + (topic to offsets.getOrDefault(topic, TopicOffsetState()).next(nextOffset))
        )

    fun commitAt(topic: Topic, lastOffset: Offset) =
        copy(
            offsets =
            offsets + (topic to offsets.getOrDefault(topic, TopicOffsetState()).commitAt(lastOffset))
        )
}
