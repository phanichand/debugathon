package com.debugathon.problem4;
import java.util.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
final class CompletionTracker {
    private final Map<TopicPartition,Long> positions=new HashMap<>();
    void record(TopicPartition partition,long offset) { positions.merge(partition,offset+1,Math::max); }
    Map<TopicPartition,OffsetAndMetadata> snapshot() {
        Map<TopicPartition,OffsetAndMetadata> result=new HashMap<>();
        positions.forEach((p,o)->result.put(p,new OffsetAndMetadata(o))); return result;
    }
    void forget(Collection<TopicPartition> partitions) { partitions.forEach(positions::remove); }
}
