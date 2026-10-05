package com.debugathon.problem4;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class KafkaSettings {
    public final String bootstrap, topic, group;
    public KafkaSettings(@Value("${platform.bootstrap}") String bootstrap,
                         @Value("${platform.topic}") String topic,
                         @Value("${platform.group}") String group) {
        this.bootstrap=bootstrap; this.topic=topic; this.group=group;
    }
    public Properties base() { Properties p=new Properties(); p.put("bootstrap.servers",bootstrap); return p; }
}
