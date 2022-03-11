package top.futurenotfound.mqtt.client;

import org.eclipse.paho.mqttv5.common.MqttMessage;
import top.futurenotfound.mqtt.client.annotation.Subscribe;

/**
 * must annotation {@code top.futurenotfound.mqtt.client.annotation.Subscribe}
 *
 * @author liuzhuoming
 * @see Subscribe
 */
public interface SubscribeHandler {

    /**
     * handle message
     *
     * @param topic   topic
     * @param message message
     */
    void onMessage(String topic, MqttMessage message);
}
