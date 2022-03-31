package top.futurenotfound.mqtt.client.store;

import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Other subscription store
 * <p>
 * k=topic,v=(k=match topic,v=list)
 *
 * @author liuzhuoming
 */
@Configuration
public class OtherSubscriptionStore extends ConcurrentHashMap<String, Set<Consumer<MqttMessage>>> {
}
