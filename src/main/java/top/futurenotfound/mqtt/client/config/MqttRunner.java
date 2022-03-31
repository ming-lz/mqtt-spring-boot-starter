package top.futurenotfound.mqtt.client.config;

import lombok.AllArgsConstructor;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.util.MqttTopicValidator;
import org.reflections.Reflections;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import top.futurenotfound.mqtt.client.SubscribeHandler;
import top.futurenotfound.mqtt.client.annotation.Subscribe;
import top.futurenotfound.mqtt.client.env.MqttSharedSubscriptionType;
import top.futurenotfound.mqtt.client.store.OtherSubscriptionStore;
import top.futurenotfound.mqtt.client.store.QueuedSubscriptionStore;
import top.futurenotfound.mqtt.client.store.SharedSubscriptionStore;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The data of the {@code top.futurenotfound.mqtt.client.annotation.Sub} annotation will stored in the {@code top.futurenotfound.mqtt.client.MessageHandlerStore}, after springboot starts successfully,.
 *
 * @author liuzhuoming
 * @see Subscribe
 */
@Configuration
@AllArgsConstructor
public class MqttRunner implements CommandLineRunner {

    private final MqttClient mqttClient;
    private final SharedSubscriptionStore sharedSubscriptionStore;
    private final QueuedSubscriptionStore queuedSubscriptionStore;
    private final OtherSubscriptionStore otherSubscriptionStore;

    @Override
    public void run(String... args) throws Exception {
        Reflections reflections = new Reflections();
        Set<Class<? extends SubscribeHandler>> subTypes = reflections.getSubTypesOf(SubscribeHandler.class);
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Subscribe.class);

        Set<Class<?>> classSet = annotated.stream()
                .filter(subTypes::contains)
                .collect(Collectors.toSet());

        Map<String, Consumer<MqttMessage>> consumerHashMap = new HashMap<>();

        Map<String, Consumer<MqttMessage>> exactMatchConsumerMap = new HashMap<>();
        Map<String, Consumer<MqttMessage>> wildcardsConsumerMap = new HashMap<>();

        for (Class<?> clazz : classSet) {
            Subscribe annotation = clazz.getAnnotation(Subscribe.class);
            String[] topics = annotation.topics();

            Method subMethod = clazz.getMethod("onMessage", String.class, MqttMessage.class);

            for (String topic : topics) {
                if (!topic.contains("#") && !topic.contains("+")) {
                    exactMatchConsumerMap.put(topic, consumer(topic, subMethod, clazz));
                } else {
                    wildcardsConsumerMap.put(topic, consumer(topic, subMethod, clazz));
                }
            }
        }

        //exactMatch first
        for (Map.Entry<String, Consumer<MqttMessage>> entry : exactMatchConsumerMap.entrySet()) {
            String topic = entry.getKey();
            Consumer<MqttMessage> consumer = entry.getValue();
            storeOtherSubscription(consumerHashMap, topic, consumer);
        }
        //wildcards
        for (Map.Entry<String, Consumer<MqttMessage>> entry : wildcardsConsumerMap.entrySet()) {
            String topic = entry.getKey();
            Consumer<MqttMessage> consumer = entry.getValue();
            storeOtherSubscription(consumerHashMap, topic, consumer);
        }

        for (Map.Entry<String, Consumer<MqttMessage>> entry : consumerHashMap.entrySet()) {
            String topic = entry.getKey();
            Consumer<MqttMessage> consumer = entry.getValue();

            if (topic.startsWith(MqttSharedSubscriptionType.QUEUE.getPrefix())
                    || topic.startsWith(MqttSharedSubscriptionType.SHARE.getPrefix())) {
                if (topic.startsWith(MqttSharedSubscriptionType.QUEUE.getPrefix())) {
                    topic = topic.substring(MqttSharedSubscriptionType.QUEUE.getPrefix().length());
                    queuedSubscriptionStore.computeIfAbsent(topic, k -> new CopyOnWriteArraySet<>())
                            .add(consumer);
                } else if (topic.startsWith(MqttSharedSubscriptionType.SHARE.getPrefix())) {
                    topic = topic.substring(topic.indexOf("/", MqttSharedSubscriptionType.SHARE.getPrefix().length()) + 1);
                    String group = topic.substring(MqttSharedSubscriptionType.SHARE.getPrefix().length(),
                            topic.indexOf("/", MqttSharedSubscriptionType.SHARE.getPrefix().length()));
                    sharedSubscriptionStore.computeIfAbsent(topic, k -> new ConcurrentHashMap<>())
                            .computeIfAbsent(group, k -> new CopyOnWriteArraySet<>())
                            .add(consumer);
                }
            }
            mqttClient.subscribe(topic, 0);
        }
    }

    private void storeOtherSubscription(Map<String, Consumer<MqttMessage>> consumerHashMap, String topic, Consumer<MqttMessage> consumer) {
        otherSubscriptionStore.computeIfAbsent(topic, k -> new CopyOnWriteArraySet<>())
                .add(consumer);

        long count = consumerHashMap.keySet().stream()
                .filter(filter -> MqttTopicValidator.isMatched(topic, filter))
                .count();
        if (count == 0) {
            consumerHashMap.put(topic, consumer);
        }
    }

    private Consumer<MqttMessage> consumer(String topic, Method subMethod, Class<?> clazz) {
        return message -> {
            try {
                subMethod.invoke(clazz.newInstance(), topic, message);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
            }
        };
    }
}
