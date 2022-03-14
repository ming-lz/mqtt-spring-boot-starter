package top.futurenotfound.mqtt.client;

import lombok.AllArgsConstructor;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.util.MqttTopicValidator;
import org.reflections.Reflections;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import top.futurenotfound.mqtt.client.annotation.Subscribe;
import top.futurenotfound.mqtt.client.env.MqttSharedSubscriptionType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
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

        Set<Class<?>> classSet = annotated.stream().filter(subTypes::contains).collect(Collectors.toSet());

        Set<String> allFilterSet = new HashSet<>();
        for (Class<?> clazz : classSet) {
            Subscribe annotation = clazz.getAnnotation(Subscribe.class);
            String[] topics = annotation.topics();
            int qos = annotation.qos();

            Method subMethod = clazz.getMethod("onMessage", String.class, MqttMessage.class);

            Set<String> filterSet = new HashSet<>();
            //exact-match topics first
            for (String topic : topics) {
                if (!topic.contains("#") && !topic.contains("+")) {
                    storeOtherSubscription(allFilterSet, filterSet, topic, subMethod, clazz);
                }
            }
            //wildcards topic
            for (String topic : topics) {
                if (topic.contains("#") || topic.contains("+")) {
                    storeOtherSubscription(allFilterSet, filterSet, topic, subMethod, clazz);
                }
            }

            for (String topic : filterSet) {
                if (topic.startsWith(MqttSharedSubscriptionType.QUEUE.getPrefix())
                        || topic.startsWith(MqttSharedSubscriptionType.SHARE.getPrefix())) {
                    if (topic.startsWith(MqttSharedSubscriptionType.QUEUE.getPrefix())) {
                        final String newTopic = topic.substring(MqttSharedSubscriptionType.QUEUE.getPrefix().length());

                        Set<Consumer<MqttMessage>> consumerSet = queuedSubscriptionStore.get(newTopic);
                        if (consumerSet == null) {
                            queuedSubscriptionStore.put(newTopic, new CopyOnWriteArraySet<>());
                        }
                        consumerSet = queuedSubscriptionStore.get(newTopic);
                        consumerSet.add(message -> {
                            try {
                                subMethod.invoke(clazz.newInstance(), newTopic, message);
                            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                                e.printStackTrace();
                            }
                        });
                        mqttClient.subscribe(newTopic, qos);
                    } else if (topic.startsWith(MqttSharedSubscriptionType.SHARE.getPrefix())) {
                        final String newTopic = topic.substring(topic.indexOf("/", MqttSharedSubscriptionType.SHARE.getPrefix().length()) + 1);
                        final String group = topic.substring(MqttSharedSubscriptionType.SHARE.getPrefix().length(),
                                topic.indexOf("/", MqttSharedSubscriptionType.SHARE.getPrefix().length()));

                        ConcurrentHashMap<String, Set<Consumer<MqttMessage>>> consumerMap = sharedSubscriptionStore.get(newTopic);
                        if (consumerMap == null) {
                            sharedSubscriptionStore.put(newTopic, new ConcurrentHashMap<>());
                        }
                        consumerMap = sharedSubscriptionStore.get(newTopic);
                        Set<Consumer<MqttMessage>> consumerSet = consumerMap.get(group);
                        if (consumerSet == null) {
                            consumerMap.put(group, new CopyOnWriteArraySet<>());
                        }
                        consumerSet = consumerMap.get(group);
                        consumerSet.add(message -> {
                            try {
                                subMethod.invoke(clazz.newInstance(), newTopic, message);
                            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                                e.printStackTrace();
                            }
                        });
                        mqttClient.subscribe(newTopic, qos);
                    }
                } else {
                    mqttClient.subscribe(topic, qos);
                }
            }
        }
    }

    private void storeOtherSubscription(Set<String> allFilterSet, Set<String> filterSet, String topic, Method subMethod, Class<?> clazz) {
        Set<Consumer<MqttMessage>> consumerSet = otherSubscriptionStore.get(topic);
        if (consumerSet == null) {
            otherSubscriptionStore.put(topic, new CopyOnWriteArraySet<>());
        }
        otherSubscriptionStore.get(topic).add(message -> {
            try {
                subMethod.invoke(clazz.newInstance(), topic, message);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
            }
        });

        long count = allFilterSet.stream()
                .filter(filter -> MqttTopicValidator.isMatched(topic, filter))
                .count();
        if (count == 0) {
            filterSet.add(topic);
            allFilterSet.add(topic);
        }
    }
}
