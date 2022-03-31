package top.futurenotfound.mqtt.client.client;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.util.MqttTopicValidator;
import org.springframework.context.annotation.Configuration;
import top.futurenotfound.mqtt.client.store.OtherSubscriptionStore;
import top.futurenotfound.mqtt.client.store.QueuedSubscriptionStore;
import top.futurenotfound.mqtt.client.store.SharedSubscriptionStore;

import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * mqtt callback
 *
 * @author liuzhuoming
 * @see MqttClientCallback#messageArrived(java.lang.String, org.eclipse.paho.mqttv5.common.MqttMessage)
 */
@Slf4j
@AllArgsConstructor
@Configuration
public class MqttClientCallback implements MqttCallback {

    private final SharedSubscriptionStore sharedSubscriptionStore;
    private final QueuedSubscriptionStore queuedSubscriptionStore;
    private final OtherSubscriptionStore otherSubscriptionStore;

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        //handle queued subscription
        if (queuedSubscriptionStore.get(topic) != null) {
            Set<Consumer<MqttMessage>> consumerSet = queuedSubscriptionStore.get(topic);
            Consumer<MqttMessage> consumer = randomElement(consumerSet);
            consumer.accept(message);
        }
        //handle shared subscription
        if (sharedSubscriptionStore.get(topic) != null) {
            ConcurrentHashMap<String, Set<Consumer<MqttMessage>>> groupMap = sharedSubscriptionStore.get(topic);
            for (Map.Entry<String, Set<Consumer<MqttMessage>>> groupEntry : groupMap.entrySet()) {
                Set<Consumer<MqttMessage>> consumerSet = groupEntry.getValue();
                Consumer<MqttMessage> consumer = randomElement(consumerSet);
                consumer.accept(message);
            }
        }
        //handle other subscription
        otherSubscriptionStore.entrySet().stream()
                .filter(entry -> MqttTopicValidator.isMatched(entry.getKey(), topic))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .forEach(consumer -> consumer.accept(message));
    }

    private <T> T randomElement(Collection<T> collection) {
        Random random = new Random();
        return collection.stream()
                .skip(random.nextInt(collection.size()))
                .findFirst().orElseThrow(RuntimeException::new);
    }

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        log.info("Mqtt server disconnected");
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
        exception.printStackTrace();
        log.info("Mqtt server mqttErrorOccurred");
    }

    @Override
    public void deliveryComplete(IMqttToken token) {
        log.info("Mqtt server deliveryComplete");
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info("Mqtt server connectComplete");
    }

    @Override
    public void authPacketArrived(int reasonCode, org.eclipse.paho.mqttv5.common.packet.MqttProperties properties) {
        log.info("Mqtt server authPacketArrived");
    }
}
